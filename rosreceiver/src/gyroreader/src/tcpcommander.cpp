#include <stdio.h>
#include <stdlib.h>
#include <string>
#include <iostream>

#include <netinet/in.h>
#include <netinet/tcp.h>
#include <sys/socket.h>
#include <unistd.h>

#include <ros/ros.h>
#include <mavros_msgs/CommandBool.h>
#include <mavros_msgs/SetMode.h>
#include <mavros_msgs/State.h>
#include <mavros_msgs/PositionTarget.h>

#define PORT 8888
int server_fd, new_socket, valread;
struct sockaddr_in address;
int opt = 1;
int addrlen = sizeof(address);
char buffer[33] = {0};

float euler[3]={0.0,0.0,0.0};
float thrust=0.0;

void deadZone(float *val, const float lim){
    *val = (abs(*val)<lim) ? 0 : (*val-lim)*(180.0/(180.0-lim)); 
    return;
}

void limitTo(float *val, const float lim){
    if (abs(*val)>lim) *val= (val<0)?-lim:lim;
    return;
}

void substr(float* fl, char* arr, const int start, const int end){
    char dst[9]={0};
    for (int i=0;i<end-start;i++){dst[i]=arr[i+start];}
    *fl = std::stof(dst);
    return;
}

void parseBuffer(){
    if (buffer[0]==0) {return;}
    substr(&thrust, buffer,0,8); //thrust
    substr(euler, buffer,8,16); //yaw in degree
    substr(euler+1, buffer,16,24); //pitch in degree
    substr(euler+2, buffer,24,32); //roll in degeree

    limitTo(euler,180); deadZone(euler,20);
    limitTo(euler+1,90); deadZone(euler+1,10);
    limitTo(euler+2,90); deadZone(euler+2,10);
    return;
}


void openServer(){
    //Initializing the socket
    if ((server_fd = socket(AF_INET, SOCK_STREAM, 0)) < 0)
    { perror("socket failed"); exit(EXIT_FAILURE);}

    //Set the socket options
    if (setsockopt(server_fd, IPPROTO_TCP,TCP_NODELAY, &opt,sizeof(opt)))
    { perror("setsockopt"); exit(EXIT_FAILURE);}

    //Configuring the address
    address.sin_family = AF_INET;
    address.sin_addr.s_addr = INADDR_ANY;
    address.sin_port = htons(PORT);

    //Binding the address to the socket
    if (bind(server_fd, (struct sockaddr *)&address, sizeof(address)) < 0)
    { perror("bind failed"); exit(EXIT_FAILURE);}

    //Listening to the socket
    if (listen(server_fd, 16) < 0)
    { perror("listen"); exit(EXIT_FAILURE);}
    
    return;
}


void readData(){
    if ((new_socket = accept(server_fd, (struct sockaddr *)&address,(socklen_t *)&addrlen)) < 0)
    { perror("accept"); exit(EXIT_FAILURE);}
    //Accept and update the buffer with the data from the client
    valread = read(new_socket, buffer, 32);
    
    parseBuffer();
    return;
}

void closeServer(){
    //close the socket
    close(new_socket);
    shutdown(server_fd, SHUT_RDWR);
    return;
}

mavros_msgs::State current_state;
void state_cb(const mavros_msgs::State::ConstPtr& msg){
    current_state = *msg;
}

mavros_msgs::PositionTarget commandedVel;
void updatePos(){
    commandedVel.velocity.x = thrust*5;
    commandedVel.velocity.y = (euler[2]/180)*5;
    commandedVel.velocity.z = -(euler[1]/180)*5;
    commandedVel.yaw_rate = euler[0]/180*5;
    ROS_INFO("%f %f %f %f",thrust,euler[3]/180,euler[2]/180,euler[1]/180);
}

int main(int argc, char **argv)
{
    openServer();

    ros::init(argc, argv, "uuh");
    ros::NodeHandle nh;

    ros::Subscriber state_sub = nh.subscribe<mavros_msgs::State>
            ("mavros/state", 10, state_cb);
    ros::ServiceClient arming_client = nh.serviceClient<mavros_msgs::CommandBool>
            ("mavros/cmd/arming");
    ros::ServiceClient set_mode_client = nh.serviceClient<mavros_msgs::SetMode>
            ("mavros/set_mode");

    //publish to local setpoint raw
    ros::Publisher vel_pub = nh.advertise<mavros_msgs::PositionTarget>
            ("mavros/setpoint_raw/local", 10);
    
    //the setpoint publishing rate MUST be faster than 2Hz
    ros::Rate rate(20.0);

    // wait for FCU connection
    while(ros::ok() && !current_state.connected){
        ros::spinOnce();
        rate.sleep();
    }

    commandedVel.coordinate_frame =mavros_msgs::PositionTarget::FRAME_BODY_NED;
    commandedVel.type_mask = mavros_msgs::PositionTarget::IGNORE_PX | mavros_msgs::PositionTarget::IGNORE_PY |
                  mavros_msgs::PositionTarget::IGNORE_PZ | mavros_msgs::PositionTarget::IGNORE_AFX |
                  mavros_msgs::PositionTarget::IGNORE_AFY | mavros_msgs::PositionTarget::IGNORE_AFZ |
                  mavros_msgs::PositionTarget::IGNORE_YAW ;
    

    for(int i = 100; ros::ok() && i > 0; --i){
        //local_pos_pub.publish(commandedP);
        ros::spinOnce();
        rate.sleep();
    }

    mavros_msgs::SetMode offb_set_mode;
    offb_set_mode.request.custom_mode = "OFFBOARD";

    mavros_msgs::CommandBool arm_cmd;
    arm_cmd.request.value = true;

    ros::Time last_request = ros::Time::now();

    while (ros::ok())
    {
        if( current_state.mode != "OFFBOARD" &&
            (ros::Time::now() - last_request > ros::Duration(5.0))){
            if( set_mode_client.call(offb_set_mode) &&
                offb_set_mode.response.mode_sent){
                ROS_INFO("Offboard enabled");
            }
            last_request = ros::Time::now();
        } else {
            if( !current_state.armed &&
                (ros::Time::now() - last_request > ros::Duration(5.0))){
                if( arming_client.call(arm_cmd) &&
                    arm_cmd.response.success){
                    ROS_INFO("Vehicle armed");
                }
                last_request = ros::Time::now();
            }
        }

        readData();
        updatePos();

        vel_pub.publish(commandedVel);

        ros::spinOnce();
        rate.sleep();
    }

    closeServer();
    return 0;
}