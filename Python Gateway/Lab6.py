import serial.tools.list_ports
import time
import sys
from Adafruit_IO import MQTTClient

AIO_FEED_ID = "bbc-temp"
AIO_USERNAME = "haily835"
AIO_KEY = "aio_xNKp90vzzUnYtb9xYzHoBvnP9fRk"

def connected(client):
    print("Connect successfully   ...")
    client.subscribe(AIO_FEED_ID)

def subscribe(client, userdata, mid, granted_qos):
    print("Subscribe succesfully...")

def disconnected(client):
    print("Disconnect...")
    sys.exit(1)

def message(client, feed_id, payload):
    print("Receive data: " + payload)
    ser.write((str(payload) + "#").encode())

def getPort():
    ports = serial.tools.list_ports.comports() # return the list of comport
    
    # loop through the list and find the comport
    N = len(ports)
    commPort = "None"
    for i in range(0, N):
        port = ports[i]
        strPort = str(port)
        if "Virtual Serial Port" in strPort:
            splitPort = strPort.split(" ")
            commPort = (splitPort[0])
    
    return commPort

def processData(data):
    # data in the form !1:TEMP:22# is sent by Microbit
    # 1 is the ID of the gateway
    # ! mark the beginning, # end 
    # extract the number 22 and publish to the Adafruit
    data = data.replace("!", "")
    data = data.replace("#", "")
    splitData = data.split(":")
    print(splitData)
    if splitData[1] == "TEMP":
        client.publish("bbc-temp", splitData[2])

mess = ""
ser = serial.Serial( port=getPort(), baudrate=115200)
def readSerial():
    bytesToRead = ser.inWaiting()
    if (bytesToRead > 0):
        global mess
        mess = mess + ser.read(bytesToRead).decode("UTF-8")
        while ("#" in mess) and ("!" in mess):
            start = mess.find("!")
            end = mess.find("#")
            processData(mess[start: end+1])
            
            if (end == len(mess)):
                # if data -----# then reset mess to begin new message
                mess = ""
            else:
                # if data "!1:TEMP:22#!2:TEMP" => reset mess = "!2:TEMP"
                mess = mess[end+1:]

client = MQTTClient(AIO_USERNAME , AIO_KEY)
client.on_connect = connected
client.on_disconnect = disconnected
client.on_message = message
client.on_subscribe = subscribe
client.connect()
client.loop_background()



while True:
    readSerial()
    time.sleep(1)