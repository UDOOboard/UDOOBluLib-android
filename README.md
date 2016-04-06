# UDOOBluLib

Library for control Udoo Blu board (bluetooth low energy)
(http://http://www.udoo.org/wp-content/uploads/2014/12/logoogo.png)

# Usage

*For a working implementation of this project see the `example/`.*

  1. Include the library as local library project or add the dependency in your build.gradle.
        repositories {
            maven {
                url  "http://dl.bintray.com/harlem88/maven"
            }
        }

        ...

        dependencies {
            compile 'org.udoo:udooblulib:0.1'
        }

  2. In your `onCreate` method in Application class, bind the `UdooBluManager`.

             @Override
             public void onCreate() {
                 super.onCreate();
                 mUdooBluManager = new UdooBluManager(this);

             }

             public UdooBluManager getBluManager(){
                 return mUdooBluManager;
             }

  3. Connect ble device:

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

  4. Enable notification

            udooBluManager.enableSensor(address1, UDOOBLESensor.ACCELEROMETER, true);
            udooBluManager.setNotificationPeriod(address1, UDOOBLESensor.ACCELEROMETER);

  5. Listen notification

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