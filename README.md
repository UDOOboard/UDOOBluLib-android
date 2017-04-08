# UDOOBluLib

![alt tag](http://www.udoo.org/wp-content/uploads/2014/12/logoogo.png)

Library for Udoo Blu board 

# Usage

  1. Include the library as local library project or add the dependency in your build.gradle.
        
        repositories {
            maven {
                url  "http://dl.bintray.com/udooboard/maven"
            }
        }

        ...

        dependencies {
            compile 'org.udoo:udooblulib:0.4.2'
        }
      
  2. Add in your `AndroidManifest.xml` UdooBluService class
        
            <service
              android:name="org.udoo.udooblulib.service.UdooBluService"
              android:exported="false" />
      This enable our app to communicate with the service that manage all the connected UDOO BLU.

  3. **Main Activity**  
     The MainActivity must extend UdooBluAppCompactActivity. It automatically checks all the permissions (Android 6 it shows all the pop-ups), and starts the UDOO BLE service.
     When all these checks are done the Activity call the onUdooBluManagerReady(). In this sample we decided to start the UDOO BLU devices scan ( startScan() method ).
     When a new UDOO BLU is detected the Activity calls.
     
            public void onBluDiscovered(int rssi, BluetoothDevice udooBluDevice) {
                ...
            }
            
  4. Connect ble device:  
            
            public void onBluDiscovered(int rssi, BluetoothDevice udooBluDevice) {
         
            if (!isConnected) {
                connect(udooBluDevice.getAddress());
                stopScan();
                isConnected = true;
                }
            } 
   The connect method accept as input the device address. We can get the address by the udooBluDevice object calling getAddress() method.
   We can manage when the scan is terminated or it’s failed.
    
        @Override
        public void onBluScanFinished() {
        ...
        }
    
        @Override
        public void onBluScanFailed() {
        ...
        }

  5. A UDOO BLU is connected
  When the connection is established the activity calls onBluConnected(final UdooBlu udooBlu) and we can work with udooBlu object.  
  
  6. Single pin
        
        udooBlu.setPinMode(<PIN_NUMBER>, IOPin.MODE.DIGITAL_OUTPUT);
  
  6.1 Multiple pins
        
        udooBlu.setIoPinMode(null, 
        IOPin.BuilderIOPin.IOPIN_PIN.A0,  IOPin.MODE.DIGITAL_OUTPUT),
  	    IOPin.Builder(IOPin.IOPIN_PIN.A2, IOPin.IOPIN_MODE.ANALOG)
        );
  
  6.1.1 Callback mode
  
        udooBlu.setIoPinMode(new OnBluOperationResult<Boolean>() {
        @Override
        public void onSuccess(Boolean aBoolean) {
  		...
        }
  
        @Override
        public void onError(UdooBluException runtimeException) {
  
        }
        }, 
        IOPin.Builder(IOPin.PIN.A0, IOPin.MODE.DIGITAL_OUTPUT),
        IOPin.Builder(IOPin.PIN.A2, IOPin.MODE.ANALOG_INPUT)
        );
  
  6.2 Read digital pin
  6.2.1 Single pin
        
        udooBlu.digitalRead(new OnBluOperationResult<Boolean>() {
        @Override
        public void onSuccess(Boolean aBoolean) {
  		...
        }
  
        @Override
        public void onError(UdooBluException runtimeException) {
  
        }
        }, 
        <PIN_NUMBER>
        );
  
  6.2.2 Multiple pin
        Write digital pin
        
        udooBlu.digitalWrite(0, 1);    // HIGH
        udooBlu.digitalWrite(0, 0); 	  // LOW
  
   Or if we call this method inside the UdooBluAppCompatActivity
        
        udooBlu.digitalWrite(0, HIGH);    // HIGH
        udooBlu.digitalWrite(0, LOW ); 	  // LOW
  
  Or generic
  
        udooBlu.digitalWrite(0, IOPin.DIGITAL_VALUE.HIGH);    // HIGH
        udooBlu.digitalWrite(0, IOPin.DIGITAL_VALUE.LOW));   // LOW
  
  7. Subscribe notifications
  7.1 Digital read
  
          udooBlu.subscribeNotificationDigital(new INotificationListener<byte[]>() {
            @Override
            public void onNext(byte[] value) {
              boolean[] pins= UDOOBLESensor.IOPIN_DIGITAL.convertIOPinDigital(value, d1);
            }
          
            @Override
            public void onError(UdooBluException runtimeException) {
          
            }
          });
  
  
  7.2 Analog Read
          
          udooBlu.subscribeNotificationDigital(new INotificationListener<byte[]>() {
            @Override
            public void onNext(byte[] value) {
              boolean[] pins= UDOOBLESensor.IOPIN_ANALOG.convertADC(value);
            }
          
            @Override
            public void onError(UdooBluException runtimeException) {
          
            }
          });
  
  8. UDOO BRICK sensor
  
          udooBlu.subscribeNotificationTemperature(new INotificationListener<byte[]>() {
                  @Override
                  public void onNext(byte[] value) {
                      float temp = UDOOBLESensor.TEMPERATURE.convertTemp(value);
                  }
  
                  @Override
                  public void onError(UdooBluException runtimeException) {
  
                  }
              }, 2000);