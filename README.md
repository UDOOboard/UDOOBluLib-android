# UDOOBluLib

![alt tag](http://www.udoo.org/wp-content/uploads/2014/12/logoogo.png)

Library for Udoo Blu board 

# Usage

  1. Include the library as local library project or add the dependency in your build.gradle.
        
        repositories {
            maven {
                url  "http://dl.bintray.com/harlem88/maven"
            }
        }

        ...

        dependencies {
            compile 'org.udoo:udooblulib:0.1.2'
        }
      
  2. Add in your `AndroidManifest.xml` UdooBluService class
        
            <service
              android:name="org.udoo.udooblulib.service.UdooBluService"
              android:exported="false" />

  3. In your `onCreate` method in Application class, bind the `UdooBluManager`.

             @Override
             public void onCreate() {
                 super.onCreate();
                 mUdooBluManager = new UdooBluManager(this);

             }

             public UdooBluManager getBluManager(){
                 return mUdooBluManager;
             }

  4. Connect ble device:

            mUdooBluManager.connect(address1, new IBleDeviceListener() {
                        @Override
                        public void onDeviceConnected() {
                            udooBluManager.discoveryServices(address1);
                        }

                        @Override
                        public void onServicesDiscoveryCompleted() {
                            lunchGloveFragment(address1, address2);
                        }

                        @Override
                        public void onDeviceDisconnect() {

                        }
                    });

  5. Enable notifications

            udooBluManager.enableSensor(address1, UDOOBLESensor.ACCELEROMETER, true);
            udooBluManager.setNotificationPeriod(address1, UDOOBLESensor.ACCELEROMETER);

  6. Listen notifications

            udooBluManager.enableNotification(address1, true, UDOOBLESensor.ACCELEROMETER, new OnCharacteristicsListener() {
                                @Override
                                public void onCharacteristicsRead(String uuidStr, byte[] value, int status) {
                                }

                                @Override
                                public void onCharacteristicChanged(String uuidStr, byte[] rawValue) {
                                    Point3D point3D = UDOOBLESensor.ACCELEROMETER.convert(rawValue);
                                    if (point3D != null)
                                        subscriber.onNext(point3D.toFloatArray());
                                }
                            });
  7. Digital write
            
            mUdooBluManager.digitalWrite(address1, IOPIN_VALUE.HIGH, IOPIN.D6);
