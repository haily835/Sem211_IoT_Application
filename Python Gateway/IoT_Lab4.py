import random
import time
import sys

from Adafruit_IO import MQTTClient

AIO_FEED_ID = "bbc-temp"
AIO_USERNAME = "haily835"
AIO_KEY = "aio_xNKp90vzzUnYtb9xYzHoBvnP9fRk"

def connected(client):
    print("Ket noi thanh cong...")
    client.subscribe(AIO_FEED_ID)

def subscribe(client, userdata, mid, granted_qos):
    print("Subcribe thanh cong...")

def disconnected(client):
    print("Ngat ket noi...")
    sys.exit(1)

def message(client, feed_id, payload):
    print("Nhan du lieu: " + payload)

def tempSentSuccess(client, feed_id, payload):
    global isSent
    isSent = True
    print("Da gui thanh cong du lieu: " + payload)

client = MQTTClient(AIO_USERNAME, AIO_KEY)
client.on_connect = connected
client.on_disconnect = disconnected
client.on_message = tempSentSuccess
client.on_subscribe = subscribe
client.connect()
client.loop_background()

isSent = False

while True: 
    isSent = False

    value = random.randint(0, 100)
    
    print("Cap nhat: ", value)
    client.publish("bbc-temp", value)

    # try 2 times
    i = 1
    while i >= 0:
        t_end = time.time() + 2 # run the loop for 2 seconds
        while time.time() < t_end:
            if isSent == True:
                break
        if (isSent == True):
            print("gui thanh cong lan ", i)
            break
        i -= 1
    
    time.sleep(10)