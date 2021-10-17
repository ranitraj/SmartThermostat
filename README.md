# SmartThermostat
This repository is an Android mobile application which demonstrates the mechanism to receive data from the inbuilt sensors (temperature and humidity)
embeddded in an Arduino-Nano 33 and display the same in the Android device using Bluetooth LE.

## Peripheral and Client
### Peripheral
  * Arduino-Nano 33 BLE Sense, which advertises data. (GATT Server) [Refer: BLE_Thermostat.ino file in ArduinoBlePeripheralForAndroid repository]
### Client
  * The mobile application, which receives data advertised by the peripheral device. (GATT Client) [Refer: Current Repository]
  
## Features in the application:
  1. Scan for BLE devices broadcasting Environmental-Sensing Service
  2. Connect to the BLE device
  3. Receive data advertised by the peripheral
     * Environmental-Sensing Service
        * Temperature (Characteristic Property: Read and Notify)
        * Humidity (Characteristic Property: Read and Notify)
     * Device-Information Service
        * Manufacturer Name (Characteristic Property: Read)
        * Manufacturer Model (Characteristic Property: Read)
     * LED Service (Custom Service)
        * LED Status (Characteristic Property: Write)
        
## Implementation:
  * Activity:
     * MainActivity - Allows user to scan for BLE devices of type Environmental-Sensing via Dialog and display them in BleDeviceAdapter along with data from BLE peripheral
  * Service:
     * BleConnectivityService (Bounded-service) - Performs asynchronous operations of connecting, disconnecting and receiving data from the peripheral and, 
     communicates with the UI via Broadcast-Receiver
  * Broadcast-Receivers:
     * AclBroadcastReceiver - Broadcasts global events of connection and disconnection of BLE devices
     
 ## Screenshots:
![WhatsApp Image 2021-06-28 at 21 27 43](https://user-images.githubusercontent.com/15179100/123692082-05338300-d874-11eb-88d3-6671c5a370d8.jpeg)
![WhatsApp Image 2021-06-28 at 21 27 53](https://user-images.githubusercontent.com/15179100/123692076-0369bf80-d874-11eb-81bc-3fcf16934ce6.jpeg)
![WhatsApp Image 2021-06-28 at 21 28 03](https://user-images.githubusercontent.com/15179100/123692068-019ffc00-d874-11eb-8b5d-3efa5277ce1b.jpeg)
![WhatsApp Image 2021-06-28 at 21 28 15](https://user-images.githubusercontent.com/15179100/123692060-ffd63880-d873-11eb-8470-c4dc35f86319.jpeg)
![WhatsApp Image 2021-06-28 at 21 28 25](https://user-images.githubusercontent.com/15179100/123692205-27c59c00-d874-11eb-95d2-d989cf6b7c6b.jpeg)
![WhatsApp Image 2021-06-28 at 21 28 35](https://user-images.githubusercontent.com/15179100/123692209-2a27f600-d874-11eb-911a-ac8b1b125f43.jpeg)
![WhatsApp Image 2021-06-28 at 21 28 44](https://user-images.githubusercontent.com/15179100/123692219-2c8a5000-d874-11eb-963e-c229f18fc21c.jpeg)
![WhatsApp Image 2021-06-28 at 21 28 54](https://user-images.githubusercontent.com/15179100/123692223-2dbb7d00-d874-11eb-8ca2-832d151ce316.jpeg)

## License:
Copyright 2021, Ranit Raj Ganguly

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
