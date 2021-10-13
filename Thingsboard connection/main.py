import paho.mqtt.client as mqttclient
import time
import json

BROKER_ADDRESS = "demo.thingsboard.io"
PORT = 1883
THINGS_BOARD_ACCESS_TOKEN = "HHXw2glNOY1Hyz7wWD5F" # go to things board and copy

def subscribed(client, userdata, mid, granted_qos):
    print("Subscribed...")

def recv_message(client, userdata, message):
    print("Received: ", message.payload.decode("utf-8"))
   
    try:
        jsonobj = json.loads(message.payload)
        if jsonobj['method'] == "setLedValue":
            led_data = {'led_value': jsonobj['params']}
            client.publish('v1/devices/me/attributes', json.dumps(led_data), 1)
        if jsonobj['method'] == "setSwitchValue":
            switch_data = {'switch_value': jsonobj['params']}
            client.publish('v1/devices/me/attributes', json.dumps(switch_data), 1)
    except:
        pass

def  connected(client, usedata, flags, rc):
    if rc == 0:
        print("Thingsboard connected successfully!!")
        client.subscribe("v1/devices/me/rpc/request/+")
    else:
        print("Connection is failed")

client = mqttclient.Client("Gateway_Thingsboard")
client.username_pw_set(THINGS_BOARD_ACCESS_TOKEN)

client.on_connect = connected
client.connect(BROKER_ADDRESS, 1883)
client.loop_start()


client.on_subscribe = subscribed
client.on_message = recv_message

temp = 30
humi = 50
counter = 0
while True:
    counter +=1
    if(counter >=10):
        counter = 0
        collect_data = {'temperature': temp, 'humidity': humi, 'lat':10.2, 'lon':106.2}
        temp += 1
        humi += 1
        client.publish('v1/devices/me/telemetry', json.dumps(collect_data), 1)

    time.sleep(1)



