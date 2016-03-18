package com.punchthrough.bean.sdk.internal.upload.firmware;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;

import com.punchthrough.bean.sdk.internal.ble.GattClient;
import com.punchthrough.bean.sdk.internal.exception.ImageParsingException;
import com.punchthrough.bean.sdk.message.BeanError;
import com.punchthrough.bean.sdk.message.Callback;
import com.punchthrough.bean.sdk.message.UploadProgress;
import com.punchthrough.bean.sdk.upload.FirmwareBundle;
import com.punchthrough.bean.sdk.upload.FirmwareImage;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.punchthrough.bean.sdk.internal.utility.Convert.intArrayToByteArray;
import static junit.framework.TestCase.fail;
import static org.assertj.core.api.Assertions.assertThat;
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

    // Mocks
    GattClient mockGattClient;
    BluetoothGattService mockOADService;
    BluetoothGattCharacteristic mockOADIdentify;
    BluetoothGattCharacteristic mockOADBlock;
    BluetoothGattDescriptor mockOADIdentifyDescriptor;
    BluetoothGattDescriptor mockOADBlockDescriptor;
    List<BluetoothGattService> services = new ArrayList<>();

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

        // Instantiate class under test
        oadProfile = new OADProfile(mockGattClient);

        // Customize some behavior
        when(mockGattClient.isConnected()).thenReturn(true);
        when(mockGattClient.getServices()).thenReturn(services);
        when(mockGattClient.getService(SERVICE_OAD)).thenReturn(mockOADService);
        when(mockGattClient.setCharacteristicNotification(mockOADIdentify, true)).thenReturn(true);
        when(mockGattClient.setCharacteristicNotification(mockOADBlock, true)).thenReturn(true);
        when(mockOADService.getCharacteristic(CHAR_OAD_IDENTIFY)).thenReturn(mockOADIdentify);
        when(mockOADService.getCharacteristic(CHAR_OAD_BLOCK)).thenReturn(mockOADBlock);
        when(mockOADIdentify.getDescriptor(CLIENT_CHAR_CONFIG)).thenReturn(mockOADIdentifyDescriptor);
        when(mockOADBlock.getDescriptor(CLIENT_CHAR_CONFIG)).thenReturn(mockOADBlockDescriptor);
    }

    @Test
    public void testProgramWithFirmware() throws ImageParsingException {

        final List<BeanError> errors = new ArrayList<>();

        FirmwareImage imageA = new FirmwareImage(rawImageA, "");
        FirmwareImage imageB = new FirmwareImage(rawImageB, "");
        List<FirmwareImage> images = new ArrayList<>();
        images.add(imageA);
        images.add(imageB);
        FirmwareBundle bundle = new FirmwareBundle(images);

        Callback<UploadProgress> onProgress = new Callback<UploadProgress>() {
            @Override
            public void onResult(UploadProgress result) {

            }
        };

        Runnable onComplete = new Runnable() {
            @Override
            public void run() {

            }
        };

        Callback<BeanError> onError = new Callback<BeanError>() {
            @Override
            public void onResult(BeanError result) {
                errors.add(result);
            }
        };

        oadProfile.programWithFirmware(bundle, onProgress, onComplete, onError);

        for (BeanError e : errors) {
            fail(e.toString());
        }

        assertThat(oadProfile.getState()).isEqualTo(FirmwareUploadState.AWAIT_CURRENT_HEADER);

    }

}
