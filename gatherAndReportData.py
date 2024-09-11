#!/bin/python3

import time
import board
import uuid
import busio
import adafruit_bme280
import serial 
import bluetooth
import socket
import numpy as np
import w1thermsensor
from w1thermsensor import W1ThermSensor, Sensor
from socket import error as SocketError
import errno
from threading import Thread
import threading

import adafruit_ads1x15.ads1115 as ADS
from adafruit_ads1x15.analog_in import AnalogIn

#initialize server socket
server_sock = bluetooth.BluetoothSocket( bluetooth.RFCOMM )

#lock = threading.Lock()

# Shared with the android app, hardcoded for now in both
sharedUUID = "146e39ac-103d-416b-9b3d-93c1596724c3"

# setup i2c comms
i2c = busio.I2C(board.SCL, board.SDA)

# setup analog to digital converter
ads = ADS.ADS1115(i2c)

# set gain to 2/3 since measured voltage can be over 5 V
ads.gain = 2/3

# init bme280 sensor for weather data
bme280 = adafruit_bme280.Adafruit_BME280_I2C(i2c)
bme280.sea_level_pressure = 1013.25

# init connection to controller
ser = serial.Serial("/dev/serial0", 9600)
ser.timeout = 1.0

# init connect to display
serDisplay = serial.Serial("/dev/ttyAMA1", 9600)
serDisplay.timeout = 1.0

#commented out for now, could add back in at some point
#brakeTempSensor = W1ThermSensor(Sensor.DS18B20, "00000d09c7b7")
#batteryTempSensor = W1ThermSensor(Sensor.DS18B20, "00000d0bb438")

# analog input set to position 3
chan = AnalogIn(ads, ADS.P3)

#default values to send, for now
m_BrakeTemp = 0.0
m_BatteryTemp = 0.0

# go through the process of setting up the bluetooth connection
# with the android app
def connect():
	server_sock = bluetooth.BluetoothSocket( bluetooth.RFCOMM )
	server_sock.bind(("",bluetooth.PORT_ANY))
	server_sock.listen(1)
	bluetooth.advertise_service( server_sock, "Rad City Data", sharedUUID )

	client_sock,address = server_sock.accept()
	print("Accepted connection from ",address)
	
	return client_sock

# hit all sensors and interpret messages from the ebike
def gatherData(): 
	ambientTemp = bme280.temperature
	ambientHumidity = bme280.relative_humidity
	ambientPressure = bme280.pressure
	ambientAlt = bme280.altitude
 
	# controller data
	ser.flushInput()
	received_data = ser.read().hex()
	time.sleep(0.03)
	data_left = ser.inWaiting()
	received_data += ser.read(data_left).hex()

	requestedAmps = 0
	
	# should always be 8 bytes, throw out otherwise
	if (len(received_data) == 16):
		subStr = received_data[4:6]
		requestedAmps = int(subStr, 16)
		
	# display data
	serDisplay.flushInput()
	received_data = serDisplay.read().hex()
	time.sleep(0.03)
	data_left = serDisplay.inWaiting()
	received_data += serDisplay.read(data_left).hex()
	
	PASLevel = 0
	HeadlightState = 0
	
	# should always be 6 bytes, throw out otherwise
	if (len(received_data) == 12):
		subStr = received_data[2:4]
		byte = int(subStr, 16)
		
		# bit shifting and masking pulled from open source firmware
		# for king meter J-LCD display: https://github.com/stancecoke/BMSBattery_S_controllers_firmware/blob/Master/display_kingmeter.c
		PASLevel = byte & 0x07
		HeadlightState = (byte & 0x80) >> 7
		
	batteryVoltage = chan.voltage * 11
	brakeTemp = 0.0
	batTemp = 0.0
	
	# commented out for now, temp sensors are slow so we're using threading here
	#with lock:
		#brakeTemp = m_BrakeTemp
		#batTemp = m_BatteryTemp
	
	data = "{:.2f},{:.2f},{:.2f},{:.2f},{:.2f},{:.2f},{:.2f},{:.2f},{:.2f},{:.2f}".format(ambientTemp,
				ambientHumidity, ambientPressure, ambientAlt, 
				requestedAmps, brakeTemp, batTemp, batteryVoltage, PASLevel, HeadlightState)
	return data
 
# Send data on the socket
def reportData(sock, data):
	print(data)
	try:
		sock.send(bytes(data, 'UTF-8'))
		return True
	except SocketError as e:
		print("ran into an error, but we will just try again")
		return False
		
# Method for getting temp probe temps, currently disabled
def getProbeTemps():
	while True:
		global m_BrakeTemp
		global m_BatteryTemp
		with lock:
			try:
				m_BrakeTemp = brakeTempSensor.get_temperature()
				m_BatteryTemp = batteryTempSensor.get_temperature()
			except (w1thermsensor.core.UnsupportedUnitError, w1thermsensor.core.NoSensorFoundError, w1thermsensor.core.SensorNotReadyError,  w1thermsensor.core.ResetValueError) as e:
				print("ran into temp sensor error")
				pass
			
		time.sleep(10)
	
# Main loop to connect to phone and report data
if __name__ == "__main__":
	sock = connect()
	#thread = Thread(target=getProbeTemps, args=())
	#thread.start()
	
	while True:
		data = gatherData()
		if(reportData(sock, data)):
			time.sleep(.2)
		else:
			sock.close()
			server_sock.close()
			sock = connect()
	
