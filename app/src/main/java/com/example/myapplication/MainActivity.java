package com.example.myapplication;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.android.material.navigation.NavigationBarView;
import com.google.android.material.resources.MaterialAttributes;

import org.w3c.dom.Text;

import java.sql.Struct;
import java.util.Collection;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {
    //对于组件的调控
    public static List<Button> levelupButton=new LinkedList<>(); //记录所有向上的按钮
    public static List<Button> leveldownButton=new LinkedList<>(); //记录所有向下的按钮
    public static List<Button> elevator_button=new LinkedList<>(); //记录所有电梯内按钮
    public static List<TextView>text_elevator=new LinkedList<>(); //记录电梯标签
    public static List<Job>request_queue=new LinkedList<>(); //用来记录未能处理的任务
    //用于调度的结构
    public Map<Integer,Boolean>levelUp=new HashMap<>();  //记录每层向上楼层按钮按下状态
    public Map<Integer,Boolean>levelDown=new HashMap<>();  //记录每层向下楼层按钮按下状态
    public Map<Integer,Elevator>elevatorGroup=new HashMap<>(); //记录所有电梯对象
    public Map<Integer,Integer>up_request=new HashMap<>(); //记录向上调度楼层对应的电梯
    public Map<Integer,Integer>down_request=new HashMap<>(); //记录向下调度楼层对应的电梯

    //其他常量
    private ArrayAdapter<String> arr_adapter;  //spinner的适配器
    private final int ELEVATOR_NUM=5;   //电梯常量为5
    private final int FLOOR_NUM=20;     //楼层数量为20
    public enum Status{pause,up,down};  //枚举类型，代表电梯运行的三种状态
    public enum Order{up,down,free};    //枚举类型，代表电梯当前执行的任务
    public int option_elevator=1;  //表示当前选中电梯
    public int selected_number=1;   //表示当前选中楼层
    public Map<Integer,Boolean>lock_door=new HashMap<>(); //防止开门时调度
    public class Job    //用于处理外部请求的结构体
    {
         public int requestLevel;
         public boolean wantUp;
         public boolean hashFinished;
    }

    public class Elevator   // 电梯类，代表每部电梯的内部状况
    {
        public Status status = Status.pause; //电梯初始状况为暂停和释放
        public Order order = Order.free;
        public Map<Integer, Boolean> destination=new HashMap<Integer, Boolean>();
        public int currentFloor=1; // 代表当前楼层
        public int no;//电梯编号
        public Elevator(int elevatorNo) {
            for (int i = 1; i <= FLOOR_NUM; i++) {
                destination.put(i, false);
            }
            no=elevatorNo;
           // destination.put(3,true);  一一对应，可覆盖

        }


    }

    public void renew_outside(int number)  //更新电梯外部按钮状态，每次切换楼层时改变可视化
    {
        boolean up_able=true,down_able=true; //代表电梯外部状态
        if(levelUp.get(number)) //向上按钮转为橙色
            up_able=false;
        else
            up_able=true;
        if(levelDown.get(number))
            down_able=false;
        else
            down_able=true;
        if(number==1)
            down_able=false;
        else if(number==FLOOR_NUM)
            up_able=false;
        if(!up_able) {    //遍历设置向上按钮状态
            for (int i = 0; i < levelupButton.size(); i++) {
                levelupButton.get(i).setBackgroundColor(0xffcc6633);
            }
        }
        else{
            for (int i = 0; i < levelupButton.size(); i++) {
                levelupButton.get(i).setBackgroundColor(0xffCCCCCC);
            }
        }
        if(!down_able) {   //遍历设置向下按钮状态
            for (int i = 0; i < leveldownButton.size(); i++) {
                leveldownButton.get(i).setBackgroundColor(0xffcc6633);
            }
        }
        else {
            for (int i = 0; i < leveldownButton.size(); i++) {
                leveldownButton.get(i).setBackgroundColor(0xffCCCCCC);
            }
        }
    }

    public void renew_inside(int number)  //更新电梯内部按钮状态,表示切换到第number部电梯内部
    {

        //btn.setText(String.valueOf(number));
        Elevator chosen_elevator=elevatorGroup.get(number); //扎到对应电梯
        for(int i=1;i<=FLOOR_NUM;i++)
        {
            Button btn=elevator_button.get(i-1);
            if(chosen_elevator.destination.get(i))  //假如第i各按钮为true，标亮他
            {
               btn.setBackgroundColor(0xffcc6633);
            }
            else
                btn.setBackgroundColor(0xffCCCCCC);


        }
    }
    public void renew_text(int elevator_no)  //更新电梯对应标签
    {
        Elevator currentElevator=elevatorGroup.get(elevator_no);
        String mid=String.valueOf(currentElevator.currentFloor);
        if(currentElevator.status==Status.pause&&currentElevator.order!=Order.free)
            text_elevator.get(elevator_no-1).setText("open");
        else if(currentElevator.status==Status.up&&currentElevator.order==Order.up)
            text_elevator.get(elevator_no-1).setText(mid+"↑");
        else if(currentElevator.status==Status.down&&currentElevator.order==Order.down)
            text_elevator.get(elevator_no-1).setText(mid+"↓");
        else if(currentElevator.status==Status.pause&&currentElevator.order==Order.free)
            text_elevator.get(elevator_no-1).setText(mid);
    }
    public void buttonout_click(Button btn){  //电梯外部按钮被按下后执行该监听函数
        if(btn.getText().toString().contains("↑"))   //假如向上按钮被按下
        {
            levelUp.put(selected_number,true); //标亮当前楼层向上按钮
            out_dispatch(selected_number,true);
        }
        else
        {
            levelDown.put(selected_number,true);  //标亮当前楼层向下按钮
            out_dispatch(selected_number,false);
        }
        renew_outside(selected_number); //更新外部按钮
    }
    public void buttoninner_click(int number){  //电梯内部被按下后执行该监听函数
    Elevator current_elevator=elevatorGroup.get(this.option_elevator);  //获取当前被选中电梯
    inner_dispatch(current_elevator,number); //内部调度

    }

    public void check_ahead(Elevator currentElevator)   //检查每次停靠后电梯是否需要继续运行
    {
        boolean up = false, down = false;
        for (int i = 1; i <= FLOOR_NUM; i++) {
            if (currentElevator.destination.get(i))   //电梯仍需运行
            {
                if (i < currentElevator.currentFloor) //假如有下降需求
                    down = true;
                else if (i > currentElevator.currentFloor)  //假如有上升需求
                    up = true;
                else  //假设处于当前楼层
                    currentElevator.destination.put(i, false);
            }
        }
        if(up_request.containsKey(currentElevator.currentFloor)&&up_request.get(currentElevator.currentFloor)==currentElevator.no)
        {
            levelUp.put(currentElevator.currentFloor,false);
            up_request.remove(currentElevator.currentFloor);  //完成后将其从请求队列内清除
            renew_outside(currentElevator.currentFloor);   //完成后更新外部楼层按钮状况
        }
        else if(down_request.containsKey(currentElevator.currentFloor)&&down_request.get(currentElevator.currentFloor)==currentElevator.no)
        {
            levelDown.put(currentElevator.currentFloor,false);
            down_request.remove(currentElevator.currentFloor);  //完成后将其从请求队列内清除
            renew_outside(currentElevator.currentFloor);   //完成后更新外部楼层按钮状况
        }
        if(!up&&down)  //进行状态转移
        {
            currentElevator.order=Order.down;
            currentElevator.status=Status.down;
        }
        else if(up&&!down)  //继续上升
        {
            currentElevator.order=Order.up;
            currentElevator.status=Status.up;
        }
        else if(currentElevator.order==Order.up&&up)
            currentElevator.status=Status.up;   //电梯继续上升
        else if(currentElevator.order==Order.down&&down)
            currentElevator.status=Status.down;  //电梯继续下降
        else if(!up&&!down)
            currentElevator.order=Order.free;  //电梯状态释放
    }
    public void check_pause(Elevator currentElevator)  //检查当前楼层是否需要停靠
    {
      if(currentElevator.destination.get(currentElevator.currentFloor))
        {
            currentElevator.destination.put(currentElevator.currentFloor,false);  //取消该按钮
            currentElevator.status=Status.pause;
        }
    }
    /*核心调度算法
     */
    public void inner_dispatch(Elevator currentElevator,int level)  //内部请求调度
    {
        currentElevator.destination.put(level,true);
        if(currentElevator.order==Order.free)  //电梯刚刚起步
        {
                if(level>currentElevator.currentFloor)   //电梯应上升
                    currentElevator.order=Order.up;
                else if(level<currentElevator.currentFloor)  //电梯应下降
                    currentElevator.order=Order.down;
                else //电梯应开门
                {
                    int No=currentElevator.no;
                    //该电梯时间控件暂停一秒，记得加上
                    text_elevator.get(No-1).setText("open");  //文本设置为open

                    currentElevator.destination.put(level,false);
                    //text_elevator.get(No-1).setText(String.valueOf(level));
                }
                renew_inside(currentElevator.no);
        }
    }
    public void out_dispatch(int requestlevel,boolean direction)
    {
        int chosenElevator=0; //用于选定相应电梯
        int distance=FLOOR_NUM;
        for(int i=1;i<=elevatorGroup.size();i++)  //出错记得检查一下
        {
            boolean flag=false;
            Elevator elevator=elevatorGroup.get(i);
            int current_distance=elevator.currentFloor-requestlevel;
            if(elevator.order== Order.free&&((direction&&!down_request.containsValue(elevator.no))||
                    (!direction&&!up_request.containsValue(elevator.no))))
            {
               current_distance=Math.abs(current_distance);
               flag=true;
            }
            //电梯有上升任务，且请求为上升，且电梯并未相应下降请求
            else if(elevator.order==Order.up&&direction&&!down_request.containsValue(elevator.no))
            {
                current_distance=-current_distance;
                flag=true;
            }
            //电梯有下降任务，且请求为下降，且电梯未相应上升请求
            else if(elevator.order==Order.down&&!direction&&!up_request.containsValue(elevator.no))
            {
                flag=true;
            }
            if(flag&&current_distance<distance&&current_distance>0)
            {
                distance=current_distance;
                //此处将distance值改变，从而保证了距离该楼层间隔最小的电梯被调度
                chosenElevator=i;
            }
            else if(elevator.status==Status.pause&&current_distance==0)  //假如恰好赶上电梯
            {
                int no=elevator.no;
                String mid=text_elevator.get(i-1).getText().toString(); //list记得减一
                if(mid!="open")
                {
                    text_elevator.get(i-1).setText("open");
                }
                  //text_elevator.get(i-1).setText(String.valueOf(requestlevel));  //回归正常状态
                if(direction)
                    levelUp.put(requestlevel,false);
                else
                    levelDown.put(requestlevel,false);
                renew_outside(i);
                return;

            }
        }
        if(chosenElevator!=0)
        {
            elevatorGroup.get(chosenElevator).destination.put(requestlevel,true);
            //将任务记录写入表中
            if(direction&&!up_request.containsKey(requestlevel))
                up_request.put(requestlevel,chosenElevator);
            else if(!direction&&!down_request.containsKey(requestlevel))
                down_request.put(requestlevel,chosenElevator);
            Job job=new Job();
            job.requestLevel=requestlevel;
            job.wantUp=direction;
            job.hashFinished=false;
            if(judge(job)!=-1)
            {
                int s=judge(job);
                //Button btn=findViewById(R.id.button8);
                //btn.setText(String.valueOf(s));
                job.hashFinished=true;
                request_queue.set(s,job);
                //待议
            }
        }
        else  //假如未找到合适的电梯，则将任务加入等待队列
        {
            Job waiting=new Job();
            waiting.requestLevel=requestlevel;
            waiting.wantUp=direction;
            waiting.hashFinished=false;
            if(judge(waiting)==-1)
                request_queue.add(waiting);
        }
    }
    public int judge(Job job){
        for(int i=0;i<request_queue.size();i++){
            Job mid=request_queue.get(i);
            if(job.hashFinished==mid.hashFinished&&job.wantUp==mid.wantUp&&job.requestLevel==mid.requestLevel)
                return i;
        }
        return -1;
    }
    public void spinner1()  //初始化spinner对象
    {
        Spinner spinner=(Spinner) findViewById(R.id.spinner);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String str=parent.getItemAtPosition(position).toString();
                String regEx="[^0-9]";
                Pattern p= Pattern.compile(regEx);
                Matcher m=p.matcher(str);
                String str1=m.replaceAll("").trim();
                int number=Integer.valueOf(str1); //字符串不是纯数字时用这种方法
                selected_number=number;  //表明当前选中楼层
                renew_outside(number);  //监听下拉列表事件

            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selected_number=1;
            }
        });
    }
        public void start_Init()  //初始化所有存储对象
        {
            option_elevator=1;
            for(int i=1;i<=FLOOR_NUM;i++)  //初始电梯外部按钮状态
            {
                levelUp.put(i,false);
                levelDown.put(i,false);
            }
            levelupButton.add((Button)findViewById(R.id.button21));
            levelupButton.add((Button)findViewById(R.id.button22));
            levelupButton.add((Button)findViewById(R.id.button23));
            levelupButton.add((Button)findViewById(R.id.button24));
            levelupButton.add((Button)findViewById(R.id.button25));  //绑定向上的按钮
            leveldownButton.add((Button)findViewById(R.id.button27));
            leveldownButton.add((Button)findViewById(R.id.button28));
            leveldownButton.add((Button)findViewById(R.id.button29));
            leveldownButton.add((Button)findViewById(R.id.button30));
            leveldownButton.add((Button)findViewById(R.id.button26));  //绑定向下的按钮
            //绑定电梯内楼层按钮
            elevator_button.add((Button) findViewById(R.id.button1));
            elevator_button.add((Button) findViewById(R.id.button2));
            elevator_button.add((Button) findViewById(R.id.button3));
            elevator_button.add((Button) findViewById(R.id.button4));
            elevator_button.add((Button) findViewById(R.id.button5));
            elevator_button.add((Button) findViewById(R.id.button6));
            elevator_button.add((Button) findViewById(R.id.button7));
            elevator_button.add((Button) findViewById(R.id.button8));
            elevator_button.add((Button) findViewById(R.id.button9));
            elevator_button.add((Button) findViewById(R.id.button10));
            elevator_button.add((Button) findViewById(R.id.button11));
            elevator_button.add((Button) findViewById(R.id.button12));
            elevator_button.add((Button) findViewById(R.id.button13));
            elevator_button.add((Button) findViewById(R.id.button14));
            elevator_button.add((Button) findViewById(R.id.button15));
            elevator_button.add((Button) findViewById(R.id.button16));
            elevator_button.add((Button) findViewById(R.id.button17));
            elevator_button.add((Button) findViewById(R.id.button18));
            elevator_button.add((Button) findViewById(R.id.button19));
            elevator_button.add((Button) findViewById(R.id.button20));
            /*########*/
            text_elevator.add((TextView) findViewById(R.id.textView4));
            text_elevator.add((TextView) findViewById(R.id.textView5));
            text_elevator.add((TextView) findViewById(R.id.textView6));
            text_elevator.add((TextView) findViewById(R.id.textView7));
            text_elevator.add((TextView) findViewById(R.id.textView8));
            for(int i=1;i<=ELEVATOR_NUM;i++)   //实例化电梯集合
            {
                elevatorGroup.put(i,new Elevator(i));

            }
            for(int i=0;i<elevator_button.size();i++){   //为电梯内部按钮添加监听事件
                elevator_button.get(i).setOnClickListener(new View.OnClickListener(){
                    @Override
                    public void onClick(View v) {
                        Button btn=(Button)v;
                        btn.setBackgroundColor(0xffcc6633);  //改变颜色
                        int number=Integer.parseInt(btn.getText().toString());
                        buttoninner_click(number);   //更新levelupButton和leveldownButton
                    }
                });

            }
            for(int i=0;i<levelupButton.size();i++){   //为向上按钮添加监听事件
                levelupButton.get(i).setOnClickListener(new View.OnClickListener(){
                    @Override
                    public void onClick(View v) {
                        Button btn=(Button)v;
                        buttonout_click(btn);   //添加监听函数
                    }
                });

            }
            for(int i=0;i<leveldownButton.size();i++){   //为向下按钮添加监听事件
                leveldownButton.get(i).setOnClickListener(new View.OnClickListener(){
                    @Override
                    public void onClick(View v) {
                        Button btn=(Button)v;
                        buttonout_click(btn);   //添加监听函数
                    }
                });

            }
            /*
             给门初始化，均为上锁
             */
            lock_door.put(1,true);
            lock_door.put(2,true);
            lock_door.put(3,true);
            lock_door.put(4,true);
            lock_door.put(5,true);
            Button open=(Button) findViewById(R.id.button);  //绑定开门按钮
            open.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Button open1=(Button)v;
                    Button close1=(Button)findViewById(R.id.button31);  //绑定关门
                    Button btn = (Button) findViewById(R.id.button24);
                    if (elevatorGroup.get(option_elevator).status == Status.pause) {
                        text_elevator.get(option_elevator - 1).setText("open");
                        open1.setBackgroundColor(0xffcc6633);
                        close1.setBackgroundColor(0xffCCCCCC);
                        lock_door.put(elevatorGroup.get(option_elevator).no, false);  //给门上锁
                        Timer timer11 = new Timer();
                        TimerTask time_task = new TimerTask() {
                            @Override
                            public void run() {
                                open1.setBackgroundColor(0xffCCCCCC);
                                lock_door.put(elevatorGroup.get(option_elevator).no, true);
                            }
                        };
                        timer11.schedule(time_task, 3000);
                    }
                }
            });
            Button close=(Button) findViewById(R.id.button31); //绑定关门按钮
            close.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Button close2=(Button) v;
                    Button open2=(Button) findViewById(R.id.button);
                    if(elevatorGroup.get(option_elevator).status==Status.pause) {
                        int number = elevatorGroup.get(option_elevator).currentFloor;
                        text_elevator.get(option_elevator - 1).setText(String.valueOf(number));
                        open2.setBackgroundColor(0xffCCCCCC);  //open重新变灰
                        close2.setBackgroundColor(0xffcc6633);
                        Timer timer11 = new Timer();
                        TimerTask time_task = new TimerTask() {
                            @Override
                            public void run() {
                                open2.setBackgroundColor(0xffCCCCCC);
                            }
                        };
                        timer11.schedule(time_task, 500);
                        lock_door.put(elevatorGroup.get(option_elevator).no, false);//给门解锁
                    }
                }
            });

        }
        public void radio_button(){
        RadioGroup radio=(RadioGroup) findViewById(R.id.radioGroup2);
        radio.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                RadioButton but1=(RadioButton)findViewById(checkedId);
                int current=Integer.parseInt(but1.getText().toString());
                option_elevator=current;
                renew_inside(current); //监听电梯选中事件，输入电梯楼层
            }
        });
        }
        public void timer_elevator(int number)  //用于定时操控电梯
        {
            Elevator current_elevator=elevatorGroup.get(number);
            if(!lock_door.get(number))  //表示处于开门状态
                return;
            if(current_elevator.status==Status.up)
                current_elevator.currentFloor++;
            else if(current_elevator.status==Status.down)
                current_elevator.currentFloor--;
            if(current_elevator.status==Status.pause)
            {
                check_ahead(current_elevator);
                renew_text(number);
            }
            else
            {
                check_pause(current_elevator);  //检查电梯是否停靠
                renew_text(number);   //更新电梯对应文本
            }
            if(number==option_elevator)   //假如为选中电梯内部，再改变电梯布局
            renew_inside(number);
        }
        public void  dispatch_task()   //为等待队列中的事件分配任务
        {
            int count=0;
            while(count<request_queue.size())
            {
                if(request_queue.get(count).hashFinished)
                    request_queue.remove(count);
                else
                    count++;
            }
            for(Job job:request_queue){
                out_dispatch(job.requestLevel,job.wantUp);
            }
        }
        public void timer_task()  //定时控件，按时执行相应任务
        {
            Timer timer=new Timer();
            Timer timer1=new Timer();
            Timer timer2=new Timer();
            Timer timer3=new Timer();
            Timer timer4=new Timer();
            Timer timer5=new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    dispatch_task();
                }
            },10,500);
            timer1.schedule(new TimerTask() {
                @Override
                public void run() {
                    timer_elevator(1);
                }
            },10,1000);
            timer2.schedule(new TimerTask() {
                @Override
                public void run() {
                    timer_elevator(2);
                }
            },10,1000);
            timer3.schedule(new TimerTask() {
                @Override
                public void run() {
                    timer_elevator(3);
                }
            },10,1000);
            timer4.schedule(new TimerTask() {
                @Override
                public void run() {
                    timer_elevator(4);
                }
            },10,1000);
            timer5.schedule(new TimerTask() {
                @Override
                public void run() {
                    timer_elevator(5);
                }
            },10,1000);

        }

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_main);   //绑定资源布局
            start_Init(); //初始化所有存储对象
            spinner1();
            radio_button(); //监听radioButton对象
            timer_task();
        }
}