package com.punchthrough.bean.sdk.internal.utility;

import com.punchthrough.bean.sdk.internal.exception.NoEnumFoundException;

import java.util.EnumSet;

/**
 * Utilities for parsing enums from raw values.
 */
public class EnumParse {

    /**
     * Denotes enums that provide a method "int getRawValue()" to get their unique int representation.
     */
    public static interface ParsableEnum {
        public int getRawValue();
    }

    /**
     * Retrieve the enum of a given type from a given raw value. Enums must implement the
     * {@link com.punchthrough.bean.sdk.internal.utility.EnumParse.ParsableEnum} interface to ensure they have
     * a {@link com.punchthrough.bean.sdk.internal.utility.EnumParse.ParsableEnum#getRawValue()} method.
     * <a href="http://stackoverflow.com/a/16406386/254187">Based on this StackOverflow answer.</a>
     *
     * @param enumClass The class of the enum type being parsed, e.g. <code>BeanState.class</code>
     * @param value     The raw int value of the enum to be retrieved
     * @param <T>       The enum type being parsed
     * @return          The enum value with the given raw value
     *
     * @throws com.punchthrough.bean.sdk.internal.exception.NoEnumFoundException if the given enum type has no enum value with a raw value
     *      matching the given value
     */
    public static <T extends Enum & ParsableEnum> T enumWithRawValue(Class<T> enumClass, int value)
            throws NoEnumFoundException {

        for (Object oneEnumRaw : EnumSet.allOf(enumClass)) {
            // This is an awkward hack: we _know_ T oneEnumRaw is of type T, since allOf(TClass)
            // only returns T-type enums, but allOf doesn't guarantee this
            //noinspection unchecked
            T oneEnum = (T) oneEnumRaw;
            if (value == oneEnum.getRawValue()) {
                return oneEnum;
            }
        }
        throw new NoEnumFoundException(String.format(
                "No enum found for class %s with raw value %d", enumClass.getName(), value));

    }

    /**
     * Retrieve the enum of a given type from a given raw value. Enums must implement the
     * {@link com.punchthrough.bean.sdk.internal.utility.EnumParse.ParsableEnum} interface to ensure they have
     * a {@link com.punchthrough.bean.sdk.internal.utility.EnumParse.ParsableEnum#getRawValue()} method.
     *
     * @param enumClass The class of the enum type being parsed, e.g. <code>BeanState.class</code>
     * @param value     The raw byte value of the enum to be retrieved
     * @param <T>       The enum type being parsed
     * @return          The enum value with the given raw value
     *
     * @throws com.punchthrough.bean.sdk.internal.exception.NoEnumFoundException if the given enum type has no enum value with a raw value
     *      matching the given value
     */
    public static <T extends Enum & ParsableEnum> T enumWithRawValue(Class<T> enumClass, byte value)
            throws NoEnumFoundException {

        return enumWithRawValue(enumClass, (int) value);

    }

}
