#define START_FRAME 0x77
#define COMMAND_SEND_SERIAL_MESSAGE 0
#define COMMAND_WRITE_SCRATCH 1

uint8_t arbitraryData[3] = {1, 2, 3};

void setup() {
  Serial.begin();
}

void handleCommand(int command) {
  switch (command) {
    case COMMAND_SEND_SERIAL_MESSAGE:
      Serial.write(arbitraryData, 3);
      Serial.flush();
      break;
    case COMMAND_WRITE_SCRATCH:
      Bean.setScratchData(1, arbitraryData, 3);
      break;
  }
}

void loop() {
  char buffer[2] = {0, 0};
  size_t bytesRead = Serial.readBytes(buffer, 2);

  if (bytesRead == 2) {
    if (buffer[0] == START_FRAME) {
      handleCommand(buffer[1]);
    }
  }

  Bean.sleep(5000);
}
