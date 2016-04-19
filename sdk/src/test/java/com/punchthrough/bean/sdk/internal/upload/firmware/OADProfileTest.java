package com.punchthrough.bean.sdk.internal.upload.firmware;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;

import com.punchthrough.bean.sdk.internal.ble.GattClient;
import com.punchthrough.bean.sdk.internal.device.DeviceProfile;
import com.punchthrough.bean.sdk.internal.exception.ImageParsingException;
import com.punchthrough.bean.sdk.message.BeanError;
import com.punchthrough.bean.sdk.message.Callback;
import com.punchthrough.bean.sdk.message.UploadProgress;
import com.punchthrough.bean.sdk.upload.FirmwareBundle;
import com.punchthrough.bean.sdk.upload.FirmwareImage;

import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.punchthrough.bean.sdk.internal.utility.Convert.intArrayToByteArray;
import static junit.framework.TestCase.fail;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class OADProfileTest {

    private static final UUID SERVICE_OAD = UUID.fromString("F000FFC0-0451-4000-B000-000000000000");
    private static final UUID CHAR_OAD_IDENTIFY = UUID.fromString("F000FFC1-0451-4000-B000-000000000000");
    private static final UUID CHAR_OAD_BLOCK = UUID.fromString("F000FFC2-0451-4000-B000-000000000000");
    private static final UUID CLIENT_CHAR_CONFIG = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    byte[] rawImageA = intArrayToByteArray(new int[]{
            0x2B, 0x65,                // CRC
            0xFF, 0xFF,                // CRC Shadow
            0x64, 0x00,                // Version
            0x00, 0x7C,                // Length
            0x41, 0x41, 0x41, 0x41,    // Image A (ASCII 41)
            0xFF, 0xFF, 0xFF, 0xFF     // Reserved
    });

    byte[] rawImageB = intArrayToByteArray(new int[]{
            0x2B, 0x65,                // CRC
            0xFF, 0xFF,                // CRC Shadow
            0x64, 0x00,                // Version
            0x00, 0x7C,                // Length
            0x42, 0x42, 0x42, 0x42,    // Image B (ASCII 42)
            0xFF, 0xFF, 0xFF, 0xFF     // Reserved
    });

    // Test state
    DeviceProfile.DeviceInfoCallback testDeviceInfoCallback;
    List<BeanError> testErrors = new ArrayList<>();

    // Mocks
    GattClient mockGattClient;
    BluetoothGattService mockOADService;
    BluetoothGattCharacteristic mockOADIdentify;
    BluetoothGattCharacteristic mockOADBlock;
    BluetoothGattDescriptor mockOADIdentifyDescriptor;
    BluetoothGattDescriptor mockOADBlockDescriptor;
    List<BluetoothGattService> services = new ArrayList<>();
    DeviceProfile mockDeviceProfile;

    // Class under test
    OADProfile oadProfile;

    @Before
    public void setup() {

        // Instantiate mocks
        mockGattClient = mock(GattClient.class);
        mockOADService = mock(BluetoothGattService.class);
        mockOADIdentify = mock(BluetoothGattCharacteristic.class);
        mockOADBlock = mock(BluetoothGattCharacteristic.class);
        mockOADIdentifyDescriptor = mock(BluetoothGattDescriptor.class);
        mockOADBlockDescriptor = mock(BluetoothGattDescriptor.class);
        mockDeviceProfile = mock(DeviceProfile.class);

        // Instantiate class under test
        oadProfile = new OADProfile(mockGattClient);

        // Customize some behavior
        when(mockGattClient.isConnected()).thenReturn(true);
        when(mockGattClient.getServices()).thenReturn(services);
        when(mockGattClient.getService(SERVICE_OAD)).thenReturn(mockOADService);
        when(mockGattClient.setCharacteristicNotification(mockOADIdentify, true)).thenReturn(true);
        when(mockGattClient.setCharacteristicNotification(mockOADBlock, true)).thenReturn(true);
        when(mockGattClient.getDeviceProfile()).thenReturn(mockDeviceProfile);
        when(mockOADService.getCharacteristic(CHAR_OAD_IDENTIFY)).thenReturn(mockOADIdentify);
        when(mockOADService.getCharacteristic(CHAR_OAD_BLOCK)).thenReturn(mockOADBlock);
        when(mockOADIdentify.getDescriptor(CLIENT_CHAR_CONFIG)).thenReturn(mockOADIdentifyDescriptor);
        when(mockOADBlock.getDescriptor(CLIENT_CHAR_CONFIG)).thenReturn(mockOADBlockDescriptor);

        // Grab the instance of DeviceInfoCallback so we can use it directly
        doAnswer(new Answer() {
            public Object answer(InvocationOnMock invocation) {
                Object[] args = invocation.getArguments();
                testDeviceInfoCallback = (DeviceProfile.DeviceInfoCallback) args[0];
                return null;
            }
        }).when(mockDeviceProfile).getDeviceInfo(any(DeviceProfile.DeviceInfoCallback.class));
    }

    private FirmwareBundle buildBundle() throws ImageParsingException {
        FirmwareImage imageA = new FirmwareImage(rawImageA, "12345_imageA.bin");
        FirmwareImage imageB = new FirmwareImage(rawImageB, "12345_imageB.bin");
        List<FirmwareImage> images = new ArrayList<>();
        images.add(imageA);
        images.add(imageB);
        return new FirmwareBundle(images);
    }

    private Callback<UploadProgress> onProgress = new Callback<UploadProgress>() {
        @Override
        public void onResult(UploadProgress result) {

        }
    };

    private Runnable onComplete = new Runnable() {
        @Override
        public void run() {

        }
    };

    private Callback<BeanError> onError = new Callback<BeanError>() {
        @Override
        public void onResult(BeanError result) {
            testErrors.add(result);
        }
    };

    @Test
    public void testProgramWithFirmware() throws ImageParsingException {

        // Build a firmware bundle
        FirmwareBundle bundle = buildBundle();

        // Start test
        oadProfile.programWithFirmware(bundle, onProgress, onComplete, onError);

        // Assert no errors
        for (BeanError e : testErrors) {
            fail(e.toString());
        }
    }
}
