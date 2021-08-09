import requests
import json
import logging
import traceback
import sys
from time import sleep
from datetime import datetime, timezone
from lib import get_current_window
import uuid 
import getpass

logger = logging.getLogger(__name__)

def get_active_window():
    while True:
        try:
            current_window = get_current_window()
        except Exception as e:
            logger.error('Exception thrown while trying to get active window: {}'.format(e))
            traceback.print_exc()
            current_window = { 'time': datetime.utcnow().strftime("%Y/%m/%d %H:%M:%S"), 'app': 'unknown', 'title': 'unknown'}

        if current_window is None:
            logger.debug('Unable to fetch window, trying again on next poll')
        else:
            data = {
                'time': datetime.utcnow().strftime("%Y/%m/%d %H:%M:%S"),
                'app': current_window['appname'],
                'title': current_window['title'],
            }
            return data

def window_watcher_loop(poll_time): # collect active windows for poll_time(30) seconds and send JSON to server
    counter = 0
    data = {
            'request_time': '',
            'timezone': 'UTC',
            'mac_adress': hex(uuid.getnode()),
            "computer_name": getpass.getuser(),
            'active_windows': []
        }
    while True:
        if counter < poll_time:
            data['active_windows'].append(get_active_window())
            counter += 1
            sleep(1)
        else:
            data['request_time'] = datetime.utcnow().strftime("%Y/%m/%d %H:%M:%S")
            json_data = json.dumps(data)
            try:
                response = requests.post('http://45.76.85.26:8080/active-window/add', json_data)
                # response = requests.post('http://localhost:8080/active-window/add', json_data)
                logger.warning(response) 
            except requests.exceptions.ConnectionError:
                logger.error("Cannot Connect to Server")  

            # Reset bucket and counter
            counter = 0  
            data['active_windows'].clear()
            
if __name__ == '__main__':
    window_watcher_loop(30)
