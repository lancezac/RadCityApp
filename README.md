Overview

Electric bicycles are gaining popularity in America and the rest of the world. Spurred on by the global pandemic, sales grew rapidly in 2020 and show no signs of slowing down [1]. I purchased one myself last July and have put over 800 miles on the odometer since. The extra power boost from the electric motor has allowed it to replace most short car trips, saving on gas, car wear and tear, and emissions, all while having a great time doing it. 
Most eBikes come stock with a small display that integrates with the motor controller. This display shows information about speed, miles traveled, pedal assist level, and battery charge for reference as the user rides along. 
Soon after purchasing my eBike, I became curious about how the controller worked and how information was communicated to the display. I also began to desire more information about my bike and surroundings while riding. What is my battery voltage? What is my average watt hour consumption per mile? What is the weather like where I am riding? What happens if I fall, could a friend or relative be automatically notified? Could I have an estimate of miles remaining based on my pedal assist level to know if I have enough charge to reach my destination? 
Newer, more expensive eBikes are starting to ship with mobile applications that allow users to view information about their bike and set configuration options previously only accessible through convoluted device menus. While these options are a nice and welcome addition, I wanted to see if I could expand what can be expected of an eBike companion application. In the process of achieving this goal, I would need to intercept communication between the bike controller and built-in display, publish that data to a smartphone, and display that information in an application.

Technologies Used

I leveraged several off the shelf technologies to create this system. First, I selected a Raspberry Pi 4 8GB as the device to receive and publish information from the eBike. I decided to run a stock build of Raspian as the OS and house the device in an Argon thermal case. The Pi 4 was selected because it is inexpensive, easy to prototype with, supports multiple serial channels, low power, and powerful for such a small package. The device can be seen in Figure 1. Python was used to create the script that establishes a Bluetooth connection with the mobile device, pulls information from the sensors, translates the data, and publishes the data to the device. 

![image](https://github.com/user-attachments/assets/5009af19-6f5c-4ebf-b65c-921f82c3f0b9)

Figure 1: Raspberry Pi 4 in Argon Case

Several microcontrollers were selected to gather and report data to the Raspberry Pi. Since the bike controller and display uses 5V digital logic and the Raspberry Pi uses 3.3V digital logic, a logic level shifter was used to make this translation. An example of the chip used can be seen in Figure 2. Getting the battery voltage required the use of a voltage reduction circuit and an analog to digital converter to provide the Raspberry Pi with a scalable digital signal. The complete circuit is shown in Figure 3. A BME280 was selected to gather data about ambient temperature and humidity, as well as altitude and pressure. The connected sensor is shown in Figure 4. 

![image](https://github.com/user-attachments/assets/ee0bf014-1eaa-4bda-b356-23a564907f8a)
 
Figure 2: Logic Level Shifter

![image](https://github.com/user-attachments/assets/cba3fda6-29d7-4056-8ac2-1155f026aa01)

Figure 3: Voltage Reduction Circuit and ADC

![image](https://github.com/user-attachments/assets/0ae14f06-79c9-4875-9afa-f6bce9d30902)
 
Figure 4: BME280

Having worked with Android before in an academic and professional setting, I chose to develop the mobile application for Android. Java was used in favor of Kotlin for development. When I started the project, I thought it might have been nice to learn Kotlin, but due to time constraints I stuck with Java, which I was already familiar with. Given additional time and resources, this application could also be developed for an iOS platform. 

Use Cases

The RadCityApp has four main use cases, as shown in Figure 5. 

View Bike Info

The application shows information to the user pulled from the bicycle and internal phone sensors. GPS speed, bike power state, headlight state, predicted range, pedal assist level, battery charge, battery voltage, watts sent to motor, and watt hours per mile.


View Weather Info

Temperature, humidity, altitude, and pressure information pulled from the BME280 sensor are displayed to the user. 
Configure/Send Accident Alert

The user can enable/disable the accident alert system and set a contact number in the event of a fall.
Receive Push Notifications When Range is Low

The application sends push notifications to the user when range dips below 10 and 5 miles. The notification instructs the user to decrease the pedal assist level. 

![image](https://github.com/user-attachments/assets/bdb477ef-4643-42f5-9f05-d4cdc96f3ee7)

Figure 5: RadCityApp Use Cases

Architecture

System Architecture

The RadCityApp utilizes a custom hardware system to retrieve information about the bike and environment to be published to the companion android application. Data is pulled from the bike using a Raspberry Pi and sent to the Android phone using a Bluetooth connection. The system is powered by an external battery pack and mounted to a rack on the front of the eBike. An overview of the system is shown in Figure 6.

![image](https://github.com/user-attachments/assets/b8b885ff-a91f-4f48-a23a-f820417b3ebb)

Figure 6: System Architecture Diagram

Hardware Architecture

The hardware is the key to providing real time information to the RadCityApp. Serial data flows from the RadCity controller and display into a logic level shifter to convert the 5 V logic found in the controller to the 3.3 V logic understood by the Pi. A voltage reduction circuit is used to reduce the 48 V+ battery voltage to a range from 0 to 5.5V and fed into an analog to digital converter, which produces a digital signal representative of the voltage that can be scaled to the real figure on the Pi. A BME280 is also connected to the Pi, which publishes weather data. An overview of the hardware architecture can be found in Figure 7. 

![image](https://github.com/user-attachments/assets/13419ae8-d8a0-4ff2-ac3c-0970a27b934f)
 
Figure 7: Hardware Architecture Diagram

Software Architecture

The Python script on the Raspberry Pi side handles establishing a Bluetooth connection with a smart phone, retrieving data, interpreting data, and publishing that data over Bluetooth. Many existing python libraries were leveraged to read data from the sensors and to setup the Bluetooth connection including adafruit_bme280, adafruit_ads1x15, Bluetooth, busio, serial, and socket among others.
The script on the Raspberry Pi side also has the responsibility of deciphering the messages sent from the display and controller. The message coming from the display is 6 bytes long, while the message coming from the controller is 8 bytes long. The start byte used by both messages is 0x46. The display message contains info on pedal assist level and headlight state, while the message from the controller indicates the number of amps pulled from the battery. An open-source project for a similar display model was instrumental in figuring out the start byte and bit shifting and masking required to decipher the pedal assist level and headlight state [2].
The Android application uses a simple architecture to efficiently and effectively display the data published from the Pi. When the user taps the connect button, a PiConnectThread is started. The thread establishes a connection with the Pi and starts receiving published data. Once a message has been achieved, the PiConnectThread publishes the contents to the MainActivity, which calls a function to update the front end of the application. The RangeEstimator and FortyEightVoltBatteryLevelEstimator are invoked to provide estimates based on passed in data. 
When the user taps the Accident Detection Config Button, the FallDetectionDlg is created and displayed. This dialog allows the user to turn on accident detection and provide a contact number to alert should an accident occur. When the user closes the FallDetectionDlg, the settings are saved to the FallDetection class, which monitors accelerometer data that indicates an accident has occurred. An overview of the architecture is shown in Figure 8. 

![image](https://github.com/user-attachments/assets/5ecd825e-65f5-4c6f-8a4d-fe133a769c19)

Figure 8: Software Architecture Diagram

User Interface

The main application page was designed to present information to the user as cleanly and in as large of text as possible. The option buttons at the bottom of the screen were made large and wide to allow users to easily select them while wearing bicycling gloves. See Figure 9 for a complete overview. 

![image](https://github.com/user-attachments/assets/9996fc66-e673-4ae0-9d96-b45734eca3e0)

Figure 9: Main Page

The Accident Configuration Dialog provides the user with an option to enable or disable fall detection and to provide a contact number in the case of a collision. If the event of a collision, the contact number will be sent a text message with the current GPS coordinates of the user. An example of a user session is shown in Figure 10. 

![image](https://github.com/user-attachments/assets/4c4b7b03-cd0f-431a-bbde-1653798214d0)
 
Figure 10: Accident Configuration Dialog

Challenges Faced/Lessons Learned

The biggest challenge I faced while doing this project was getting information from the bike controller and display. It took quite a bit of experimentation, research, and determination just to get to the data lines on the bike, let alone interpret their meanings. Getting battery voltage was also a major hurtle. Initially I thought the battery voltage would be in one of the messages, but after days of digging I found another line that happened to carry the voltage of the battery, also running from the controller to the display. Once I found this, I had to do some research to get a voltage reading into the Pi without destroying it or ruining electronic components along the way. Figures 11 and 12 show some of the early experimentation with the bike controller and Raspberry Pi, and Figure 13 shows the bike in its final configuration. 

![image](https://github.com/user-attachments/assets/58dcbdb2-3839-4328-9f2c-04d4a3a34bba)

Figure 11: Soldering leads to the data lines

![image](https://github.com/user-attachments/assets/d2c5a34c-9aec-44bf-abb5-b9895424f244)

Figure 12: Connecting the Pi

![image](https://github.com/user-attachments/assets/c57fc17c-d22d-43f1-bf2b-dd1b3b54484d)

Figure 13: The bike in its final configuration

Retrospective

I learned a lot about Android programming, eBikes, Raspberry Pis, and integrating hardware while implementing this project. If I had to do it all over again though, I would purchase a second eBike with the intent to hack apart its controller. I had many close calls while poking around in the controller to find the data lines, and my bike has frequently been out of commission at inconvenient times over the past few months. 

Future Work

As far as future work goes, I think the sky is the limit here. The first thing I would want to do is miniaturize this setup and pull power directly from the bike instead of using an external battery buffer. An Arduino or Raspberry Pi Pico would be a more appropriate choice for a permanent solution than a Pi 4, and I could use the hardware transition to write efficient firmware for performing the required data transfers in C or C++. This assembly could also be repackaged with the stock controller on the bike, removing the extra bulk of the front mounted basket.
Next, I would like to provide a range estimate that factors in the real time watt hours per mile and ambient temperature. I think this is certainly possible but would require more time and research, but the infrastructure is there.
In addition to the accident detection feature, I would like to add a dash cam that can automatically save off video if an accident is detected. The accident detection algorithm could also be made more complex, and the trip threshold could also be refined.
I would also like to figure out how to setup bike configuration through the serial interface and how to power on/turn off the bike. I think these would be great features to have in the app and would require deciphering more of the known messages and finding new ones that are only sent in configuration menus.  


References

[1] Toll, M., & Micah Toll @MicahToll Micah Toll is a personal electric vehicle enthusiast.    (2020, May 01). Here's why electric bike sales have skyrocketed during the coronavirus      lockdown. Retrieved from https://electrek.co/2020/05/01/electric-bike-sales-skyrocket-during-lockdown/

[2] Stancecoke. (2018, October 07). Stancecoke/BMSBattery_S_controllers_firmware. Retrieved      from https://github.com/stancecoke/BMSBattery_S_controllers_firmware/blob/Master/display_kingmeter.c
