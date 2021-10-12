import paho.mqtt.client as mqtt
import serial.tools.list_ports
import uuid
import time
import random
from queue import Queue

from pyowm.owm import OWM
import geocoder # get location


# ---------- connect to adafruit ------------------
# 1. create a client
client = mqtt.Client(str(uuid.uuid4()))

# 2. set username, key of adafruit
client.username_pw_set("haily835", "aio_xNKp90vzzUnYtb9xYzHoBvnP9fRk")

# 3.handle to set the flag when the server response.
def on_connect(client, userdata, flags, rc):
    if rc==0:
        print("Connected OK")
    else:
        print("Bad connection Returned code=",rc)
client.on_connect = on_connect

# ------------ Subscribe to topics -------------
def on_subscribe(client, userdata, mid, granted_qos):
    print("Subscribed to topic")
    
client.on_subscribe = on_subscribe

# ------------ Listen for messages -------------
def on_message(client, userdata, message):
    print("Received from feed {0} value: {1}".format(message.topic , str(message.payload.decode("utf-8"))))
    if isMicrobitConnected:
        ser.write((str(message.payload) + "#").encode())
client.on_message = on_message

#------------serial connection-----------------------------
def getPort():
    ports = serial.tools.list_ports.comports()
    N = len(ports)
    commPort = "None"
    for i in range(0, N):
        port = ports[i]
        strPort = str(port)
        if "com0com" in strPort:
            splitPort = strPort.split(" ")
            commPort = (splitPort[0])
    return commPort

isMicrobitConnected = False
if getPort() != "None":
    print("Connect to comport: ", getPort())
    ser = serial.Serial( port=getPort(), baudrate=115200)
    isMicrobitConnected = True


# examples of data in hercules: !ID:FEED:VALUE##
# !1:TEMP:55##!2:HUM:75##!3:SW:1##!4:LED:0
def processData(data):
    data = data.replace("!", "")
    data = data.replace("#", "")
    splitData = data.split(":")
    print(splitData)
    if splitData[1] == "TEMP":
        client.publish('{0}/feeds/{1}'.format('haily835', 'bbc-temp',), payload=splitData[2])
    if splitData[1] == "SW":
        client.publish('{0}/feeds/{1}'.format('haily835', 'bbc-switch',), payload=splitData[2])
    if splitData[1] == "HUMI":
        client.publish('{0}/feeds/{1}'.format('haily835', 'bbc-hum',), payload=splitData[2])
    if splitData[1] == "LED":
        client.publish('{0}/feeds/{1}'.format('haily835', 'bbc-led',), payload=splitData[2])

mess = ""
def readSerial():
    bytesToRead = ser.inWaiting()
    if (bytesToRead > 0):
        global mess
        mess = mess + ser.read(bytesToRead).decode("UTF-8")
        while ("#" in mess) and ("!" in mess):
            start = mess.find("!")
            end = mess.find("#")
            processData(mess[start:end + 1])
            if (end == len(mess)):
                mess = ""
            else:
                mess = mess[end+1:]


client.connect("io.adafruit.com")
# adafruit io format
client.subscribe('{0}/feeds/{1}'.format('haily835', 'bbc-temp'), 0)
client.subscribe('{0}/feeds/{1}'.format('haily835', 'bbc-switch'), 0)
client.subscribe('{0}/feeds/{1}'.format('haily835', 'bbc-hum'), 0)
client.subscribe('{0}/feeds/{1}'.format('haily835', 'bbc-led'), 0)
client.loop_start()


#--------------- get weather data in python------------
weather_timer = 30
owm = OWM('d841bb957d163d5d135e9f91267b8517')
mgr = owm.weather_manager()

location = geocoder.ip('me')
address = location.geojson['features'][0]['properties']['address']
lat = location.latlng[0]
lon = location.latlng[1]

owm = OWM('d841bb957d163d5d135e9f91267b8517')
mgr = owm.weather_manager()

def checkWeatherAPI():
    global weather_timer
    if weather_timer == 30:
        one_call = mgr.one_call(lat=lat, lon=lon)

        weather = {
            "temp": str(one_call.current.temperature(unit='celsius')['temp']),
            "feels_like": str(one_call.current.temperature(unit='celsius')['feels_like']),
            "humidity": str(one_call.current.humidity),
            "wind": str(one_call.current.wind()),
            "status": str(one_call.current.status),
            "last_check": str(one_call.current.reference_time(timeformat='iso')),
            "location": address,
        }

        client.publish('{0}/feeds/{1}'.format('haily835', 'bbc-weather',), payload=str(weather))

        weather_timer = 0

    else:
        weather_timer += 1


while True:
    if isMicrobitConnected:
        readSerial()
        
    checkWeatherAPI()
    
    time.sleep(1)