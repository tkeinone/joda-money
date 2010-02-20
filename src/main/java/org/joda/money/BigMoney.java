/*
 *  Copyright 2009-2010 Stephen Colebourne
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.joda.money;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.Iterator;

/**
 * An amount of money with unrestricted decimal place precision.
 * <p>
 * This class represents a quantity of money, stored as a {@code BigDecimal} amount
 * in a single {@link CurrencyUnit currency}.
 * <p>
 * Every currency has a certain standard number of decimal places.
 * This is typically 2 (Euro, British Pound, US Dollar) but might be
 * 0 (Japanese Yen), 1 (Vietnamese Dong) or 3 (Bahrain Dinar).
 * The {@code BigMoney} class is not restricted to the standard decimal places
 * and can represent an amount to any precision that a {@code BigDecimal} can represent.
 * <p>
 * BigMoney is immutable and thread-safe.
 */
public final class BigMoney implements BigMoneyProvider, Comparable<BigMoneyProvider>, Serializable {

    /**
     * The serialisation version.
     */
    private static final long serialVersionUID = 1L;

    /**
     * The currency, not null.
     */
    private final CurrencyUnit iCurrency;
    /**
     * The amount, not null.
     */
    private final BigDecimal iAmount;

    //-----------------------------------------------------------------------
    /**
     * Obtains an instance of {@code BigMoney} from a {@code BigDecimal}.
     * <p>
     * This allows you to create an instance with a specific currency and amount.
     * The scale of the money will be that of the BigDecimal.
     *
     * @param currency  the currency, not null
     * @param amount  the amount of money, not null
     * @return the new instance, never null
     */
    public static BigMoney of(CurrencyUnit currency, BigDecimal amount) {
        MoneyUtils.checkNotNull(currency, "Currency must not be null");
        MoneyUtils.checkNotNull(amount, "Amount must not be null");
        if (amount.getClass() != BigDecimal.class) {
            BigInteger value = amount.unscaledValue();
            if (value == null ) {
                throw new IllegalArgumentException("Illegal BigDecimal subclass");
            }
            if (value.getClass() != BigInteger.class) {
                value = new BigInteger(value.toString());
            }
            amount = new BigDecimal(value, amount.scale());
        }
        return new BigMoney(currency, amount);
    }

    /**
     * Obtains an instance of {@code BigMoney} from a {@code double} using a well-defined conversion.
     * <p>
     * This allows you to create an instance with a specific currency and amount.
     * <p>
     * The amount is converted via {@link BigDecimal#valueOf(double)} which yields
     * the most expected answer for most programming scenarios.
     * Any {@code double} literal in code will be converted to
     * exactly the same BigDecimal with the same scale.
     * For example, the literal '1.425d' will be converted to '1.425'.
     * The scale of the money will be that of the BigDecimal produced.
     *
     * @param currency  the currency, not null
     * @param amount  the amount of money, not null
     * @return the new instance, never null
     */
    public static BigMoney of(CurrencyUnit currency, double amount) {
        MoneyUtils.checkNotNull(currency, "Currency must not be null");
        return BigMoney.of(currency, BigDecimal.valueOf(amount));
    }

    //-----------------------------------------------------------------------
    /**
     * Obtains an instance of {@code BigMoney} from a {@code BigDecimal} at a specific scale.
     * <p>
     * This allows you to create an instance with a specific currency and amount.
     * No rounding is performed on the amount, so it must have a
     * scale less than or equal to the new scale.
     *
     * @param currency  the currency, not null
     * @param amount  the amount of money, not null
     * @param scale  the scale to use, zero or positive
     * @return the new instance, never null
     * @throws ArithmeticException if the scale exceeds the currency scale
     */
    public static BigMoney ofScale(CurrencyUnit currency, BigDecimal amount, int scale) {
        return BigMoney.ofScale(currency, amount, scale, RoundingMode.UNNECESSARY);
    }

    /**
     * Obtains an instance of {@code BigMoney} from a {@code double} using a
     * well-defined conversion, rounding as necessary.
     * <p>
     * This allows you to create an instance with a specific currency and amount.
     * If the amount has a scale in excess of the scale of the currency then the excess
     * fractional digits are rounded using the rounding mode.
     *
     * @param currency  the currency, not null
     * @param amount  the amount of money, not null
     * @param scale  the scale to use, zero or positive
     * @param roundingMode  the rounding mode to use, not null
     * @return the new instance, never null
     * @throws IllegalArgumentException if the scale is negative
     * @throws ArithmeticException if the rounding fails
     */
    public static BigMoney ofScale(CurrencyUnit currency, BigDecimal amount, int scale, RoundingMode roundingMode) {
        MoneyUtils.checkNotNull(currency, "CurrencyUnit must not be null");
        MoneyUtils.checkNotNull(amount, "Amount must not be null");
        MoneyUtils.checkNotNull(roundingMode, "RoundingMode must not be null");
        amount = amount.setScale(scale, roundingMode);
        return BigMoney.of(currency, amount);
    }

    /**
     * Obtains an instance of {@code BigMoney} from a scaled amount.
     * <p>
     * This allows you to create an instance with a specific currency, amount and scale.
     * The amount is defined in terms of the specified scale.
     * <p>
     * For example, {@code ofScale(USD, 234, 2)} creates the instance {@code USD 2.34}.
     *
     * @param currency  the currency, not null
     * @param unscaledAmount  the unscaled amount of money
     * @param scale  the scale to use
     * @return the new instance, never null
     */
    public static BigMoney ofScale(CurrencyUnit currency, long unscaledAmount, int scale) {
        MoneyUtils.checkNotNull(currency, "Currency must not be null");
        return BigMoney.of(currency, BigDecimal.valueOf(unscaledAmount, scale));
    }

    //-----------------------------------------------------------------------
    /**
     * Obtains an instance of {@code BigMoney} from an amount in major units.
     * <p>
     * This allows you to create an instance with a specific currency and amount.
     * The scale of the money will be zero.
     * <p>
     * The amount is a whole number only. Thus you can initialise the value
     * 'USD 20', but not the value 'USD 20.32'.
     * For example, {@code ofMajor(USD, 25)} creates the instance {@code USD 25}.
     *
     * @param currency  the currency, not null
     * @param amountMajor  the amount of money in the major division of the currency
     * @return the new instance, never null
     */
    public static BigMoney ofMajor(CurrencyUnit currency, long amountMajor) {
        MoneyUtils.checkNotNull(currency, "CurrencyUnit must not be null");
        return BigMoney.of(currency, BigDecimal.valueOf(amountMajor));
    }

    /**
     * Obtains an instance of {@code BigMoney} from an amount in minor units.
     * <p>
     * This allows you to create an instance with a specific currency and amount
     * expressed in terms of the minor unit.
     * The scale of the money will be that of the currency, such as 2 for USD or 0 for JPY.
     * <p>
     * For example, if constructing US Dollars, the input to this method represents cents.
     * Note that when a currency has zero decimal places, the major and minor units are the same.
     * For example, {@code ofMajor(USD, 2595)} creates the instance {@code USD 25.95}.
     *
     * @param currency  the currency, not null
     * @param amountMinor  the amount of money in the minor division of the currency
     * @return the new instance, never null
     */
    public static BigMoney ofMinor(CurrencyUnit currency, long amountMinor) {
        MoneyUtils.checkNotNull(currency, "CurrencyUnit must not be null");
        return BigMoney.of(currency, BigDecimal.valueOf(amountMinor, currency.getDecimalPlaces()));
    }

    //-----------------------------------------------------------------------
    /**
     * Obtains an instance of {@code BigMoney} representing zero.
     * <p>
     * The scale of the money will be zero.
     * For example, {@code zero(USD)} creates the instance {@code USD 0}.
     *
     * @param currency  the currency, not null
     * @return the instance representing zero, never null
     */
    public static BigMoney zero(CurrencyUnit currency) {
        return BigMoney.of(currency, BigDecimal.ZERO);
    }

    /**
     * Obtains an instance of {@code BigMoney} representing zero at a specific scale.
     * <p>
     * For example, {@code zero(USD, 2)} creates the instance {@code USD 0.00}.
     *
     * @param currency  the currency, not null
     * @param scale  the scale to use, zero or positive
     * @return the instance representing zero, never null
     * @throws IllegalArgumentException if the scale is negative
     */
    public static BigMoney zero(CurrencyUnit currency, int scale) {
        return BigMoney.of(currency, BigDecimal.valueOf(0, scale));
    }

    //-----------------------------------------------------------------------
    /**
     * Obtains an instance of {@code BigMoney} from a provider.
     * <p>
     * This allows you to create an instance from any class that implements the
     * provider, such as {@code Money}.
     * This method simply calls {@link BigMoneyProvider#toBigMoney()} checking for nulls.
     *
     * @param moneyProvider  the money to convert, not null
     * @return the new instance, never null
     */
    public static BigMoney from(BigMoneyProvider moneyProvider) {
        MoneyUtils.checkNotNull(moneyProvider, "BigMoneyProvider must not be null");
        BigMoney money = moneyProvider.toBigMoney();
        MoneyUtils.checkNotNull(money, "BigMoneyProvider must not return null");
        return money;
    }

    //-----------------------------------------------------------------------
    /**
     * Obtains an instance of {@code BigMoney} as the total value of
     * an iterable collection.
     * <p>
     * The iterable must provide at least one monetary value.
     * Subsequent amounts are added as though using {@link #plus(BigMoney)}.
     * 
     * @param monies  the non-empty iterable provider of non-null monetary values, not null
     * @return the total, never null
     * @throws java.util.NoSuchElementException if the iterable is empty
     * @throws MoneyException if the currencies differ
     */
    public static BigMoney total(Iterable<BigMoney> monies) {
        Iterator<BigMoney> it = monies.iterator();
        BigMoney total = it.next();
        MoneyUtils.checkNotNull(total, "Iterator must not contain null entries");
        while (it.hasNext()) {
            total = total.plus((BigMoney) it.next());
        }
        return total;
    }

    /**
     * Obtains an instance of {@code BigMoney} as the total value of
     * a possible empty iterable collection.
     * <p>
     * The amounts are added as though using {@link #plus(BigMoney)} starting
     * from zero in the specified currency.
     * 
     * @param currency  the currency to total in, not null
     * @param monies  the iterable provider of non-null monetary values, not null
     * @return the total, never null
     * @throws MoneyException if the currencies differ
     */
    public static BigMoney total(CurrencyUnit currency, Iterable<BigMoney> monies) {
        BigMoney total = BigMoney.zero(currency);
        for (BigMoney money : monies) {
            total = total.plus(money);
        }
        return total;
    }

    //-----------------------------------------------------------------------
    /**
     * Parses an instance of {@code BigMoney} from a string.
     * <p>
     * The string format is '<currencyCode> <amount>'.
     * The currency code must be three letters, and the amount must be a number.
     * This matches the output from {@link #toString()}.
     * <p>
     * For example, {@code parse("USD 25")} creates the instance {@code USD 25}
     * while {@code parse("USD 25.95")} creates the instance {@code USD 25.95}.
     *
     * @param moneyStr  the money string to parse, not null
     * @return the parsed instance, never null
     * @throws IllegalArgumentException if the string is malformed
     * @throws ArithmeticException if the amount is too large
     */
    public static BigMoney parse(String moneyStr) {
        MoneyUtils.checkNotNull(moneyStr, "Money must not be null");
        if (moneyStr.length() < 5 || moneyStr.charAt(3) != ' ') {
            throw new IllegalArgumentException("Money '" + moneyStr + "' cannot be parsed");
        }
        String currStr = moneyStr.substring(0, 3);
        String amountStr = moneyStr.substring(4);
        return BigMoney.of(CurrencyUnit.of(currStr), new BigDecimal(amountStr));
    }

    //-----------------------------------------------------------------------
    /**
     * Ensures that a {@code BigMoney} is not {@code null}.
     * <p>
     * If the input money is not {@code null}, then it is returned, providing
     * that the currency matches the specified currency.
     * If the input money is {@code null}, then zero money in the currency
     * is returned with a scale of zero.
     * 
     * @param money  the monetary value to check, may be null
     * @param currency  the currency to use, not null
     * @return the input money or zero in the specified currency, never null
     * @throws MoneyException if the input money is non-null and the currencies differ
     */
    public static BigMoney nonNull(BigMoney money, CurrencyUnit currency) {
        if (money == null) {
            return zero(currency);
        }
        if (money.getCurrencyUnit().equals(currency) == false) {
            MoneyUtils.checkNotNull(currency, "Currency must not be null");
            throw new MoneyException("Money does not match specified currency");
        }
        return money;
    }

    //-----------------------------------------------------------------------
    /**
     * Constructor, creating a new monetary instance.
     * 
     * @param currency  the currency to use, not null
     * @param amount  the amount of money, not null
     */
    private BigMoney(CurrencyUnit currency, BigDecimal amount) {
        assert currency != null : "Joda-Money bug: Currency must not be null";
        assert amount != null : "Joda-Money bug: Amount must not be null";
        iCurrency = currency;
        iAmount = amount;
    }

    /**
     * Resolves singletons.
     * 
     * @return the singleton instance
     */
    private Object readResolve() {
        return BigMoney.of(iCurrency, iAmount);
    }

    //-----------------------------------------------------------------------
    /**
     * Gets the currency.
     * 
     * @return the currency, never null
     */
    public CurrencyUnit getCurrencyUnit() {
        return iCurrency;
    }

    //-----------------------------------------------------------------------
    /**
     * Returns a copy of this monetary value with the specified currency.
     * <p>
     * The returned instance will have the specified currency and the amount
     * from this instance. No currency conversion or alteration to the scale occurs.
     * <p>
     * This instance is immutable and unaffected by this method.
     * 
     * @param currency  the currency to use, not null
     * @return the new instance with the input currency set, never null
     */
    public BigMoney withCurrencyUnit(CurrencyUnit currency) {
        MoneyUtils.checkNotNull(currency, "CurrencyUnit must not be null");
        if (iCurrency == currency) {
            return this;
        }
        return new BigMoney(currency, iAmount);
    }

    //-----------------------------------------------------------------------
    /**
     * Gets the scale of the {@code BigDecimal} amount.
     * <p>
     * The scale has the same meaning as in {@link BigDecimal}.
     * Positive values represent the number of decimal places in use.
     * Negative numbers represent the opposite.
     * For example, a scale of 2 means that the money will have two decimal places
     * such as 'USD 43.25'. Whereas, a scale of -3 means that only thousands can be
     * represented, such as 'GBP 124000'.
     * 
     * @return the scale in use
     * @see #withScale
     */
    public int getScale() {
        return iAmount.scale();
    }

    /**
     * Checks if this money has the scale of the currency.
     * <p>
     * Each currency has a default scale, such as 2 for USD and 0 for JPY.
     * This method checks if the current scale matches the default scale.
     * 
     * @return true if the scale equals the current default scale
     */
    public boolean isCurrencyScale() {
        return iAmount.scale() == iCurrency.getDecimalPlaces();
    }

    //-----------------------------------------------------------------------
    /**
     * Returns a copy of this monetary value with the specified scale,
     * truncating the amount if necessary.
     * <p>
     * The returned instance will have this currency and the new scaled amount.
     * For example, scaling 'USD 43.271' to a scale of 1 will yield 'USD 43.2'.
     * No rounding is performed on the amount, so it must have a
     * scale less than or equal to the new scale.
     * <p>
     * This instance is immutable and unaffected by this method.
     * 
     * @param scale  the scale to use
     * @return the new instance with the input amount set, never null
     * @throws ArithmeticException if the rounding fails
     */
    public BigMoney withScale(int scale) {
        return withScale(scale, RoundingMode.UNNECESSARY);
    }

    /**
     * Returns a copy of this monetary value with the specified scale,
     * using the specified rounding mode if necessary.
     * <p>
     * The returned instance will have this currency and the new scaled amount.
     * For example, scaling 'USD 43.271' to a scale of 1 with HALF_EVEN rounding
     * will yield 'USD 43.3'.
     * <p>
     * This instance is immutable and unaffected by this method.
     * 
     * @param scale  the scale to use
     * @param roundingMode  the rounding mode to use, not null
     * @return the new instance with the input amount set, never null
     * @throws ArithmeticException if the rounding fails
     */
    public BigMoney withScale(int scale, RoundingMode roundingMode) {
        MoneyUtils.checkNotNull(roundingMode, "RoundingMode must not be null");
        if (scale == iAmount.scale()) {
            return this;
        }
        return BigMoney.of(iCurrency, iAmount.setScale(scale, roundingMode));
    }

    //-----------------------------------------------------------------------
    /**
     * Returns a copy of this monetary value with the scale of the currency,
     * truncating the amount if necessary.
     * <p>
     * The returned instance will have this currency and the new scaled amount.
     * For example, scaling 'USD 43.271' will yield 'USD 43.27' as USD has a scale of 2.
     * No rounding is performed on the amount, so it must have a
     * scale less than or equal to the new scale.
     * <p>
     * This instance is immutable and unaffected by this method.
     * 
     * @return the new instance with the input amount set, never null
     * @throws ArithmeticException if the rounding fails
     */
    public BigMoney withCurrencyScale() {
        return withScale(iCurrency.getDecimalPlaces(), RoundingMode.UNNECESSARY);
    }

    /**
     * Returns a copy of this monetary value with the scale of the currency,
     * using the specified rounding mode if necessary.
     * <p>
     * The returned instance will have this currency and the new scaled amount.
     * For example, scaling 'USD 43.271' will yield 'USD 43.27' as USD has a scale of 2.
     * <p>
     * This instance is immutable and unaffected by this method.
     * 
     * @param roundingMode  the rounding mode to use, not null
     * @return the new instance with the input amount set, never null
     * @throws ArithmeticException if the rounding fails
     */
    public BigMoney withCurrencyScale(RoundingMode roundingMode) {
        return withScale(iCurrency.getDecimalPlaces(), roundingMode);
    }

    //-----------------------------------------------------------------------
    /**
     * Gets the amount.
     * <p>
     * This returns the value of the money as a {@code BigDecimal}.
     * The scale will be the scale of this money.
     * 
     * @return the amount, never null
     */
    public BigDecimal getAmount() {
        return iAmount;
    }

    /**
     * Gets the amount in major units as a {@code BigDecimal} with scale 0.
     * <p>
     * This returns the monetary amount in terms of the major units of the currency,
     * truncating the amount if necessary.
     * For example, 'EUR 2.35' will return 2, and 'BHD -1.345' will return -1.
     * <p>
     * This is returned as a {@code BigDecimal} rather than a {@code BigInteger}.
     * This is to allow further calculations to be performed on the result.
     * Should you need a {@code BigInteger}, simply call {@link BigDecimal#toBigInteger()}.
     * 
     * @return the major units part of the amount, never null
     */
    public BigDecimal getAmountMajor() {
        return iAmount.setScale(0, RoundingMode.DOWN);
    }

    /**
     * Gets the amount in major units as a {@code long}.
     * <p>
     * This returns the monetary amount in terms of the major units of the currency,
     * truncating the amount if necessary.
     * For example, 'EUR 2.35' will return 2, and 'BHD -1.345' will return -1.
     * 
     * @return the major units part of the amount
     * @throws ArithmeticException if the amount is too large for a {@code long}
     */
    public long getAmountMajorLong() {
        return getAmountMajor().longValueExact();
    }

    /**
     * Gets the amount in major units as an {@code int}.
     * <p>
     * This returns the monetary amount in terms of the major units of the currency,
     * truncating the amount if necessary.
     * For example, 'EUR 2.35' will return 2, and 'BHD -1.345' will return -1.
     * 
     * @return the major units part of the amount
     * @throws ArithmeticException if the amount is too large for an {@code int}
     */
    public int getAmountMajorInt() {
        return getAmountMajor().intValueExact();
    }

    /**
     * Gets the amount in minor units as a {@code BigDecimal} with scale 0.
     * <p>
     * This returns the monetary amount in terms of the minor units of the currency,
     * truncating the amount if necessary.
     * For example, 'EUR 2.35' will return 235, and 'BHD -1.345' will return -1345.
     * <p>
     * This is returned as a {@code BigDecimal} rather than a {@code BigInteger}.
     * This is to allow further calculations to be performed on the result.
     * Should you need a {@code BigInteger}, simply call {@link BigDecimal#toBigInteger()}.
     * 
     * @return the minor units part of the amount, never null
     */
    public BigDecimal getAmountMinor() {
        int cdp = getCurrencyUnit().getDecimalPlaces();
        return iAmount.setScale(cdp, RoundingMode.DOWN).movePointRight(cdp);
    }

    /**
     * Gets the amount in minor units as a {@code long}.
     * <p>
     * This returns the monetary amount in terms of the minor units of the currency,
     * truncating the amount if necessary.
     * For example, 'EUR 2.35' will return 235, and 'BHD -1.345' will return -1345.
     * 
     * @return the minor units part of the amount
     * @throws ArithmeticException if the amount is too large for a {@code long}
     */
    public long getAmountMinorLong() {
        return getAmountMinor().longValueExact();
    }

    /**
     * Gets the amount in minor units as an {@code int}.
     * <p>
     * This returns the monetary amount in terms of the minor units of the currency,
     * truncating the amount if necessary.
     * For example, 'EUR 2.35' will return 235, and 'BHD -1.345' will return -1345.
     * 
     * @return the minor units part of the amount
     * @throws ArithmeticException if the amount is too large for an {@code int}
     */
    public int getAmountMinorInt() {
        return getAmountMinor().intValueExact();
    }

    /**
     * Gets the minor part of the amount.
     * <p>
     * This return the minor unit part of the monetary amount.
     * This is defined as the amount in minor units excluding major units.
     * <p>
     * For example, EUR has a scale of 2, so the minor part is always between 0 and 99
     * for positive amounts, and 0 and -99 for negative amounts.
     * Thus 'EUR 2.35' will return 35, and 'EUR -1.34' will return -34.
     * 
     * @return the minor part of the amount, negative if the amount is negative
     */
    public int getMinorPart() {
        int cdp = getCurrencyUnit().getDecimalPlaces();
        return iAmount.setScale(cdp, RoundingMode.DOWN)
                    .remainder(BigDecimal.ONE)
                    .movePointRight(cdp).intValueExact();
    }

    //-----------------------------------------------------------------------
    /**
     * Checks if the amount is zero.
     * 
     * @return true if the amount is zero
     */
    public boolean isZero() {
        return iAmount.compareTo(BigDecimal.ZERO) == 0;
    }

    /**
     * Checks if the amount is greater than zero.
     * 
     * @return true if the amount is greater than zero
     */
    public boolean isPositive() {
        return iAmount.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Checks if the amount is zero or greater.
     * 
     * @return true if the amount is zero or greater
     */
    public boolean isPositiveOrZero() {
        return iAmount.compareTo(BigDecimal.ZERO) >= 0;
    }

    /**
     * Checks if the amount is less than zero.
     * 
     * @return true if the amount is less than zero
     */
    public boolean isNegative() {
        return iAmount.compareTo(BigDecimal.ZERO) < 0;
    }

    /**
     * Checks if the amount is zero or less.
     * 
     * @return true if the amount is zero or less
     */
    public boolean isNegativeOrZero() {
        return iAmount.compareTo(BigDecimal.ZERO) <= 0;
    }

    //-----------------------------------------------------------------------
    /**
     * Returns a copy of this monetary value with the specified amount.
     * <p>
     * The returned instance will have this currency and the new amount.
     * The scale of the returned instance will be that of the specified BigDecimal.
     * <p>
     * This instance is immutable and unaffected by this method.
     * 
     * @param amount  the monetary amount to set in the returned instance, not null
     * @return the new instance with the input amount set, never null
     */
    public BigMoney withAmount(BigDecimal amount) {
        MoneyUtils.checkNotNull(amount, "Amount must not be null");
        if (iAmount.equals(amount)) {
            return this;
        }
        return BigMoney.of(iCurrency, amount);
    }

    /**
     * Returns a copy of this monetary value with the specified amount using a well-defined
     * conversion from a {@code double}.
     * <p>
     * The returned instance will have this currency and the new amount.
     * <p>
     * The amount is converted via {@link BigDecimal#valueOf(double)} which yields
     * the most expected answer for most programming scenarios.
     * Any {@code double} literal in code will be converted to
     * exactly the same BigDecimal with the same scale.
     * For example, the literal '1.425d' will be converted to '1.425'.
     * The scale of the money will be that of the BigDecimal produced.
     * <p>
     * This instance is immutable and unaffected by this method.
     * 
     * @param amount  the monetary amount to set in the returned instance
     * @return the new instance with the input amount set, never null
     */
    public BigMoney withAmount(double amount) {
        return withAmount(BigDecimal.valueOf(amount));
    }

    //-----------------------------------------------------------------------
    /**
     * Validates that the currency of this money and the specified money match.
     * 
     * @param money  the money to check, not null
     * @throws MoneyException if the currencies differ
     */
    private BigMoney checkCurrencyEqual(BigMoneyProvider money) {
        BigMoney m = from(money);
        if (isSameCurrency(m) == false) {
            throw new MoneyException("Currencies differ: " + this + " : " + m);
        }
        return m;
    }

    //-----------------------------------------------------------------------
    /**
     * Returns a copy of this monetary value with the amount added.
     * <p>
     * This adds the specified amount to this monetary amount, returning a new object.
     * The amount added must be in the same currency.
     * <p>
     * No precision is lost in the result.
     * The scale of the result will be the maximum of the two scales.
     * For example, 'USD 25.95' plus 'USD 3.021' gives 'USD 28.971'.
     * <p>
     * This instance is immutable and unaffected by this method.
     * 
     * @param moneyToAdd  the monetary value to add, not null
     * @return the new instance with the input amount added, never null
     * @throws MoneyException if the currencies differ
     */
    public BigMoney plus(BigMoneyProvider moneyToAdd) {
        BigMoney toAdd = checkCurrencyEqual(moneyToAdd);
        return plus(toAdd.getAmount());
    }

    /**
     * Returns a copy of this monetary value with the {@code BigDecimal} amount added.
     * <p>
     * This adds the specified amount to this monetary amount, returning a new object.
     * <p>
     * No precision is lost in the result.
     * The scale of the result will be the maximum of the two scales.
     * For example, 'USD 25.95' plus '3.021' gives 'USD 28.971'.
     * <p>
     * This instance is immutable and unaffected by this method.
     * 
     * @param amountToAdd  the monetary value to add, not null
     * @return the new instance with the input amount added, never null
     */
    public BigMoney plus(BigDecimal amountToAdd) {
        MoneyUtils.checkNotNull(amountToAdd, "Amount must not be null");
        if (amountToAdd.compareTo(BigDecimal.ZERO) == 0) {
            return this;
        }
        BigDecimal amount = iAmount.add(amountToAdd);
        return BigMoney.of(iCurrency, amount);
    }

    /**
     * Returns a copy of this monetary value with the {@code double} amount added.
     * <p>
     * This adds the specified amount to this monetary amount, returning a new object.
     * <p>
     * No precision is lost in the result.
     * The scale of the result will be the maximum of the two scales.
     * For example, 'USD 25.95' plus '3.021d' gives 'USD 28.971'.
     * <p>
     * The amount is converted via {@link BigDecimal#valueOf(double)} which yields
     * the most expected answer for most programming scenarios.
     * Any {@code double} literal in code will be converted to
     * exactly the same BigDecimal with the same scale.
     * For example, the literal '1.45d' will be converted to '1.45'.
     * <p>
     * This instance is immutable and unaffected by this method.
     * 
     * @param amountToAdd  the monetary value to add, not null
     * @return the new instance with the input amount added, never null
     */
    public BigMoney plus(double amountToAdd) {
        if (amountToAdd == 0) {
            return this;
        }
        BigDecimal amount = iAmount.add(BigDecimal.valueOf(amountToAdd));
        return BigMoney.of(iCurrency, amount);
    }

    /**
     * Returns a copy of this monetary value with the amount in major units added.
     * <p>
     * This adds the specified amount in major units to this monetary amount,
     * returning a new object. The minor units will be untouched in the result.
     * <p>
     * No precision is lost in the result.
     * The scale of the result will be the maximum of the current scale and 0.
     * For example, 'USD 23.45' plus '138' gives 'USD 161.45'.
     * <p>
     * This instance is immutable and unaffected by this method.
     * 
     * @param amountToAdd  the monetary value to add, not null
     * @return the new instance with the input amount added, never null
     */
    public BigMoney plusMajor(long amountToAdd) {
        if (amountToAdd == 0) {
            return this;
        }
        BigDecimal amount = iAmount.add(BigDecimal.valueOf(amountToAdd));
        return BigMoney.of(iCurrency, amount);
    }

    /**
     * Returns a copy of this monetary value with the amount in minor units added.
     * <p>
     * This adds the specified amount in minor units to this monetary amount,
     * returning a new object.
     * <p>
     * No precision is lost in the result.
     * The scale of the result will be the maximum of the current scale and the default currency scale.
     * For example, 'USD 23.45' plus '138' gives 'USD 24.83'.
     * <p>
     * This instance is immutable and unaffected by this method.
     * 
     * @param amountToAdd  the monetary value to add, not null
     * @return the new instance with the input amount added, never null
     */
    public BigMoney plusMinor(long amountToAdd) {
        if (amountToAdd == 0) {
            return this;
        }
        BigDecimal amount = iAmount.add(BigDecimal.valueOf(amountToAdd, iCurrency.getDecimalPlaces()));
        return BigMoney.of(iCurrency, amount);
    }

    //-----------------------------------------------------------------------
    /**
     * Returns a copy of this monetary value with the amount in the same currency added
     * retaining the scale by rounding the result.
     * <p>
     * The scale of the result will be the same as the scale of this instance.
     * For example,'USD 25.95' plus 'USD 3.021' gives 'USD 28.97' with most rounding modes.
     * <p>
     * This instance is immutable and unaffected by this method.
     * 
     * @param moneyToAdd  the monetary value to add, not null
     * @param roundingMode  the rounding mode to use to adjust the scale, not null
     * @return the new instance with the input amount added, never null
     */
    public BigMoney plusRetainScale(BigMoneyProvider moneyToAdd, RoundingMode roundingMode) {
        BigMoney toAdd = checkCurrencyEqual(moneyToAdd);
        return plusRetainScale(toAdd.getAmount(), roundingMode);
    }

    /**
     * Returns a copy of this monetary value with the amount added retaining
     * the scale by rounding the result.
     * <p>
     * The scale of the result will be the same as the scale of this instance.
     * For example,'USD 25.95' plus '3.021' gives 'USD 28.97' with most rounding modes.
     * <p>
     * This instance is immutable and unaffected by this method.
     * 
     * @param amountToAdd  the monetary value to add, not null
     * @param roundingMode  the rounding mode to use to adjust the scale, not null
     * @return the new instance with the input amount added, never null
     */
    public BigMoney plusRetainScale(BigDecimal amountToAdd, RoundingMode roundingMode) {
        MoneyUtils.checkNotNull(amountToAdd, "Amount must not be null");
        if (amountToAdd.compareTo(BigDecimal.ZERO) == 0) {
            return this;
        }
        BigDecimal amount = iAmount.add(amountToAdd);
        amount = amount.setScale(getScale(), roundingMode);
        return BigMoney.of(iCurrency, amount);
    }

    /**
     * Returns a copy of this monetary value with the amount added retaining
     * the scale by rounding the result.
     * <p>
     * The scale of the result will be the same as the scale of this instance.
     * For example,'USD 25.95' plus '3.021d' gives 'USD 28.97' with most rounding modes.
     * <p>
     * The amount is converted via {@link BigDecimal#valueOf(double)} which yields
     * the most expected answer for most programming scenarios.
     * Any {@code double} literal in code will be converted to
     * exactly the same BigDecimal with the same scale.
     * For example, the literal '1.45d' will be converted to '1.45'.
     * <p>
     * This instance is immutable and unaffected by this method.
     * 
     * @param amountToAdd  the monetary value to add, not null
     * @param roundingMode  the rounding mode to use to adjust the scale, not null
     * @return the new instance with the input amount added, never null
     */
    public BigMoney plusRetainScale(double amountToAdd, RoundingMode roundingMode) {
        if (amountToAdd == 0) {
            return this;
        }
        BigDecimal amount = iAmount.add(BigDecimal.valueOf(amountToAdd));
        amount = amount.setScale(getScale(), roundingMode);
        return BigMoney.of(iCurrency, amount);
    }

    //-----------------------------------------------------------------------
    /**
     * Returns a copy of this monetary value with the amount subtracted.
     * <p>
     * This subtracts the specified amount from this monetary amount, returning a new object.
     * The amount subtracted must be in the same currency.
     * <p>
     * No precision is lost in the result.
     * The scale of the result will be the maximum of the two scales.
     * For example,'USD 25.95' minus 'USD 3.021' gives 'USD 22.929'.
     * <p>
     * This instance is immutable and unaffected by this method.
     * 
     * @param moneyToSubtract  the monetary value to subtract, not null
     * @return the new instance with the input amount subtracted, never null
     * @throws MoneyException if the currencies differ
     */
    public BigMoney minus(BigMoneyProvider moneyToSubtract) {
        BigMoney toSubtract = checkCurrencyEqual(moneyToSubtract);
        return minus(toSubtract.getAmount());
    }

    /**
     * Returns a copy of this monetary value with the {@code BigDecimal} amount subtracted.
     * <p>
     * This subtracts the specified amount from this monetary amount, returning a new object.
     * <p>
     * No precision is lost in the result.
     * The scale of the result will be the maximum of the two scales.
     * For example,'USD 25.95' minus '3.021' gives 'USD 22.929'.
     * <p>
     * This instance is immutable and unaffected by this method.
     * 
     * @param amountToSubtract  the monetary value to subtract, not null
     * @return the new instance with the input amount subtracted, never null
     */
    public BigMoney minus(BigDecimal amountToSubtract) {
        MoneyUtils.checkNotNull(amountToSubtract, "Amount must not be null");
        if (amountToSubtract.compareTo(BigDecimal.ZERO) == 0) {
            return this;
        }
        BigDecimal amount = iAmount.subtract(amountToSubtract);
        return BigMoney.of(iCurrency, amount);
    }

    /**
     * Returns a copy of this monetary value with the {@code double} amount subtracted.
     * <p>
     * This subtracts the specified amount from this monetary amount, returning a new object.
     * <p>
     * No precision is lost in the result.
     * The scale of the result will be the maximum of the two scales.
     * For example,'USD 25.95' minus '3.021d' gives 'USD 22.929'.
     * <p>
     * The amount is converted via {@link BigDecimal#valueOf(double)} which yields
     * the most expected answer for most programming scenarios.
     * Any {@code double} literal in code will be converted to
     * exactly the same BigDecimal with the same scale.
     * For example, the literal '1.45d' will be converted to '1.45'.
     * <p>
     * This instance is immutable and unaffected by this method.
     * 
     * @param amountToSubtract  the monetary value to subtract, not null
     * @return the new instance with the input amount subtracted, never null
     */
    public BigMoney minus(double amountToSubtract) {
        if (amountToSubtract == 0) {
            return this;
        }
        BigDecimal amount = iAmount.subtract(BigDecimal.valueOf(amountToSubtract));
        return BigMoney.of(iCurrency, amount);
    }

    /**
     * Returns a copy of this monetary value with the amount in major units subtracted.
     * <p>
     * This subtracts the specified amount in major units from this monetary amount,
     * returning a new object. The minor units will be untouched in the result.
     * <p>
     * No precision is lost in the result.
     * The scale of the result will be the maximum of the current scale and 0.
     * For example, 'USD 23.45' minus '138' gives 'USD -114.55'.
     * <p>
     * This instance is immutable and unaffected by this method.
     * 
     * @param amountToSubtract  the monetary value to subtract, not null
     * @return the new instance with the input amount subtracted, never null
     */
    public BigMoney minusMajor(long amountToSubtract) {
        if (amountToSubtract == 0) {
            return this;
        }
        BigDecimal amount = iAmount.subtract(BigDecimal.valueOf(amountToSubtract));
        return BigMoney.of(iCurrency, amount);
    }

    /**
     * Returns a copy of this monetary value with the amount in minor units subtracted.
     * <p>
     * This subtracts the specified amount in minor units from this monetary amount,
     * returning a new object.
     * <p>
     * No precision is lost in the result.
     * The scale of the result will be the maximum of the current scale and the default currency scale.
     * For example, USD 23.45 minus '138' gives 'USD 22.07'.
     * <p>
     * This instance is immutable and unaffected by this method.
     * 
     * @param amountToSubtract  the monetary value to subtract, not null
     * @return the new instance with the input amount subtracted, never null
     */
    public BigMoney minusMinor(long amountToSubtract) {
        if (amountToSubtract == 0) {
            return this;
        }
        BigDecimal amount = iAmount.subtract(BigDecimal.valueOf(amountToSubtract, iCurrency.getDecimalPlaces()));
        return BigMoney.of(iCurrency, amount);
    }

    //-----------------------------------------------------------------------
    /**
     * Returns a copy of this monetary value with the amount in the same currency subtracted
     * retaining the scale by rounding the result.
     * <p>
     * The scale of the result will be the same as the scale of this instance.
     * For example,'USD 25.95' minus 'USD 3.029' gives 'USD 22.92 with most rounding modes.
     * <p>
     * This instance is immutable and unaffected by this method.
     * 
     * @param moneyToSubtract  the monetary value to add, not null
     * @param roundingMode  the rounding mode to use to adjust the scale, not null
     * @return the new instance with the input amount subtracted, never null
     */
    public BigMoney minusRetainScale(BigMoneyProvider moneyToSubtract, RoundingMode roundingMode) {
        BigMoney toSubtract = checkCurrencyEqual(moneyToSubtract);
        return minusRetainScale(toSubtract.getAmount(), roundingMode);
    }

    /**
     * Returns a copy of this monetary value with the amount subtracted retaining
     * the scale by rounding the result.
     * <p>
     * The scale of the result will be the same as the scale of this instance.
     * For example,'USD 25.95' minus '3.029' gives 'USD 22.92' with most rounding modes.
     * <p>
     * This instance is immutable and unaffected by this method.
     * 
     * @param amountToSubtract  the monetary value to add, not null
     * @param roundingMode  the rounding mode to use to adjust the scale, not null
     * @return the new instance with the input amount subtracted, never null
     */
    public BigMoney minusRetainScale(BigDecimal amountToSubtract, RoundingMode roundingMode) {
        MoneyUtils.checkNotNull(amountToSubtract, "Amount must not be null");
        if (amountToSubtract.compareTo(BigDecimal.ZERO) == 0) {
            return this;
        }
        BigDecimal amount = iAmount.subtract(amountToSubtract);
        amount = amount.setScale(getScale(), roundingMode);
        return BigMoney.of(iCurrency, amount);
    }

    /**
     * Returns a copy of this monetary value with the amount subtracted retaining
     * the scale by rounding the result.
     * <p>
     * The scale of the result will be the same as the scale of this instance.
     * For example,'USD 25.95' minus '3.029d' gives 'USD 22.92' with most rounding modes.
     * <p>
     * The amount is converted via {@link BigDecimal#valueOf(double)} which yields
     * the most expected answer for most programming scenarios.
     * Any {@code double} literal in code will be converted to
     * exactly the same BigDecimal with the same scale.
     * For example, the literal '1.45d' will be converted to '1.45'.
     * <p>
     * This instance is immutable and unaffected by this method.
     * 
     * @param amountToSubtract  the monetary value to add, not null
     * @param roundingMode  the rounding mode to use to adjust the scale, not null
     * @return the new instance with the input amount subtracted, never null
     */
    public BigMoney minusRetainScale(double amountToSubtract, RoundingMode roundingMode) {
        if (amountToSubtract == 0) {
            return this;
        }
        BigDecimal amount = iAmount.subtract(BigDecimal.valueOf(amountToSubtract));
        amount = amount.setScale(getScale(), roundingMode);
        return BigMoney.of(iCurrency, amount);
    }

    //-----------------------------------------------------------------------
    /**
     * Returns a copy of this monetary value multiplied by the specified value.
     * <p>
     * No precision is lost in the result.
     * The result has a scale equal to the sum of the two scales.
     * For example, 'USD 1.13' multiplied by '2.5' gives 'USD 2.825'.
     * <p>
     * This instance is immutable and unaffected by this method.
     * 
     * @param valueToMultiplyBy  the scalar value to multiply by, not null
     * @return the new multiplied instance, never null
     */
    public BigMoney multipliedBy(BigDecimal valueToMultiplyBy) {
        MoneyUtils.checkNotNull(valueToMultiplyBy, "Multiplier must not be null");
        if (valueToMultiplyBy.compareTo(BigDecimal.ONE) == 0) {
            return this;
        }
        BigDecimal amount = iAmount.multiply(valueToMultiplyBy);
        return BigMoney.of(iCurrency, amount);
    }

    /**
     * Returns a copy of this monetary value multiplied by the specified value.
     * <p>
     * No precision is lost in the result.
     * The result has a scale equal to the sum of the two scales.
     * For example, 'USD 1.13' multiplied by '2.5' gives 'USD 2.825'.
     * <p>
     * The amount is converted via {@link BigDecimal#valueOf(double)} which yields
     * the most expected answer for most programming scenarios.
     * Any {@code double} literal in code will be converted to
     * exactly the same BigDecimal with the same scale.
     * For example, the literal '1.45d' will be converted to '1.45'.
     * <p>
     * This instance is immutable and unaffected by this method.
     * 
     * @param valueToMultiplyBy  the scalar value to multiply by, not null
     * @return the new multiplied instance, never null
     */
    public BigMoney multipliedBy(double valueToMultiplyBy) {
        if (valueToMultiplyBy == 1) {
            return this;
        }
        BigDecimal amount = iAmount.multiply(BigDecimal.valueOf(valueToMultiplyBy));
        return BigMoney.of(iCurrency, amount);
    }

    /**
     * Returns a copy of this monetary value multiplied by the specified value.
     * <p>
     * No precision is lost in the result.
     * The result has a scale equal to the scale of this money.
     * For example, 'USD 1.13' multiplied by '2' gives 'USD 2.26'.
     * <p>
     * This instance is immutable and unaffected by this method.
     * 
     * @param valueToMultiplyBy  the scalar value to multiply by, not null
     * @return the new multiplied instance, never null
     */
    public BigMoney multipliedBy(long valueToMultiplyBy) {
        if (valueToMultiplyBy == 1) {
            return this;
        }
        BigDecimal amount = iAmount.multiply(BigDecimal.valueOf(valueToMultiplyBy));
        return BigMoney.of(iCurrency, amount);
    }

    //-----------------------------------------------------------------------
    /**
     * Returns a copy of this monetary value multiplied by the specified value
     * using the specified rounding mode to adjust the scale of the result.
     * <p>
     * This multiplies this money by the specified value, retaining the scale of this money.
     * This will frequently lose precision, hence the need for a rounding mode.
     * For example, 'USD 1.13' multiplied by '2.5' and rounding down gives 'USD 2.82'.
     * <p>
     * This instance is immutable and unaffected by this method.
     * 
     * @param valueToMultiplyBy  the scalar value to multiply by, not null
     * @param roundingMode  the rounding mode to use to bring the decimal places back in line, not null
     * @return the new multiplied instance, never null
     * @throws ArithmeticException if the rounding fails
     */
    public BigMoney multiplyRetainScale(BigDecimal valueToMultiplyBy, RoundingMode roundingMode) {
        MoneyUtils.checkNotNull(valueToMultiplyBy, "Multiplier must not be null");
        MoneyUtils.checkNotNull(roundingMode, "RoundingMode must not be null");
        if (valueToMultiplyBy.compareTo(BigDecimal.ONE) == 0) {
            return this;
        }
        BigDecimal amount = iAmount.multiply(valueToMultiplyBy);
        amount = amount.setScale(getScale(), roundingMode);
        return BigMoney.of(iCurrency, amount);
    }

    /**
     * Returns a copy of this monetary value multiplied by the specified value
     * using the specified rounding mode to adjust the scale of the result.
     * <p>
     * This multiplies this money by the specified value, retaining the scale of this money.
     * This will frequently lose precision, hence the need for a rounding mode.
     * For example, 'USD 1.13' multiplied by '2.5' and rounding down gives 'USD 2.82'.
     * <p>
     * The amount is converted via {@link BigDecimal#valueOf(double)} which yields
     * the most expected answer for most programming scenarios.
     * Any {@code double} literal in code will be converted to
     * exactly the same BigDecimal with the same scale.
     * For example, the literal '1.45d' will be converted to '1.45'.
     * <p>
     * This instance is immutable and unaffected by this method.
     * 
     * @param valueToMultiplyBy  the scalar value to multiply by, not null
     * @param roundingMode  the rounding mode to use to bring the decimal places back in line, not null
     * @return the new multiplied instance, never null
     * @throws ArithmeticException if the rounding fails
     */
    public BigMoney multiplyRetainScale(double valueToMultiplyBy, RoundingMode roundingMode) {
        return multiplyRetainScale(BigDecimal.valueOf(valueToMultiplyBy), roundingMode);
    }

    //-----------------------------------------------------------------------
    /**
     * Returns a copy of this monetary value divided by the specified value
     * using the specified rounding mode to adjust the scale.
     * <p>
     * The result has the same scale as this instance.
     * For example, 'USD 1.13' divided by '2.5' and rounding down gives 'USD 0.45'
     * (amount rounded down from 0.452).
     * <p>
     * This instance is immutable and unaffected by this method.
     * 
     * @param valueToDivideBy  the scalar value to divide by, not null
     * @param roundingMode  the rounding mode to use, not null
     * @return the new divided instance, never null
     * @throws ArithmeticException if dividing by zero
     * @throws ArithmeticException if the rounding fails
     */
    public BigMoney dividedBy(BigDecimal valueToDivideBy, RoundingMode roundingMode) {
        MoneyUtils.checkNotNull(valueToDivideBy, "Divisor must not be null");
        MoneyUtils.checkNotNull(roundingMode, "RoundingMode must not be null");
        if (valueToDivideBy.compareTo(BigDecimal.ONE) == 0) {
            return this;
        }
        BigDecimal amount = iAmount.divide(valueToDivideBy, roundingMode);
        return BigMoney.of(iCurrency, amount);
    }

    /**
     * Returns a copy of this monetary value divided by the specified value
     * using the specified rounding mode to adjust the scale.
     * <p>
     * The result has the same scale as this instance.
     * For example, 'USD 1.13' divided by '2.5' and rounding down gives 'USD 0.45'
     * (amount rounded down from 0.452).
     * <p>
     * The amount is converted via {@link BigDecimal#valueOf(double)} which yields
     * the most expected answer for most programming scenarios.
     * Any {@code double} literal in code will be converted to
     * exactly the same BigDecimal with the same scale.
     * For example, the literal '1.45d' will be converted to '1.45'.
     * <p>
     * This instance is immutable and unaffected by this method.
     * 
     * @param valueToDivideBy  the scalar value to divide by, not null
     * @param roundingMode  the rounding mode to use, not null
     * @return the new divided instance, never null
     * @throws ArithmeticException if dividing by zero
     * @throws ArithmeticException if the rounding fails
     */
    public BigMoney dividedBy(double valueToDivideBy, RoundingMode roundingMode) {
        MoneyUtils.checkNotNull(roundingMode, "RoundingMode must not be null");
        if (valueToDivideBy == 1) {
            return this;
        }
        BigDecimal amount = iAmount.divide(BigDecimal.valueOf(valueToDivideBy), roundingMode);
        return BigMoney.of(iCurrency, amount);
    }

    /**
     * Returns a copy of this monetary value divided by the specified value
     * using the specified rounding mode to adjust the decimal places in the result.
     * <p>
     * The result has the same scale as this instance.
     * For example, 'USD 1.13' divided by '2' and rounding down gives 'USD 0.56'
     * (amount rounded down from 0.565).
     * <p>
     * This instance is immutable and unaffected by this method.
     * 
     * @param valueToDivideBy  the scalar value to divide by, not null
     * @param roundingMode  the rounding mode to use, not null
     * @return the new divided instance, never null
     * @throws ArithmeticException if dividing by zero
     */
    public BigMoney dividedBy(long valueToDivideBy, RoundingMode roundingMode) {
        if (valueToDivideBy == 1) {
            return this;
        }
        BigDecimal amount = iAmount.divide(BigDecimal.valueOf(valueToDivideBy), roundingMode);
        return BigMoney.of(iCurrency, amount);
    }

    //-----------------------------------------------------------------------
    /**
     * Returns a copy of this monetary value with the amount negated.
     * <p>
     * This instance is immutable and unaffected by this method.
     * 
     * @return the new instance with the amount negated, never null
     */
    public BigMoney negated() {
        if (isZero()) {
            return this;
        }
        return BigMoney.of(iCurrency, iAmount.negate());
    }

    /**
     * Returns a copy of this monetary value with a positive amount.
     * <p>
     * This instance is immutable and unaffected by this method.
     * 
     * @return the new instance with the amount converted to be positive, never null
     */
    public BigMoney abs() {
        return (isNegative() ? negated() : this);
    }

    //-----------------------------------------------------------------------
    /**
     * Returns a copy of this monetary value rounded to the specified scale without
     * changing the current scale.
     * <p>
     * Scale is described in {@link BigDecimal} and represents the point below which
     * the monetary value is zero. Negative scales round increasingly large numbers.
     * Unlike {@link #withScale(int)}, this scale of the result is unchanged.
     * <ul>
     * <li>Rounding 'EUR 45.23' to a scale of -1 returns 40.00 or 50.00 depending on the rounding mode.
     * <li>Rounding 'EUR 45.23' to a scale of 0 returns 45.00 or 46.00 depending on the rounding mode.
     * <li>Rounding 'EUR 45.23' to a scale of 1 returns 45.20 or 45.30 depending on the rounding mode.
     * <li>Rounding 'EUR 45.23' to a scale of 2 has no effect (it already has that scale).
     * <li>Rounding 'EUR 45.23' to a scale of 3 has no effect (the scale is not increased).
     * </ul>
     * This instance is immutable and unaffected by this method.
     * 
     * @param scale  the new scale
     * @param roundingMode  the rounding mode to use, not null
     * @return the new instance with the amount converted to be positive, never null
     * @throws ArithmeticException if the rounding fails
     */
    public BigMoney rounded(int scale, RoundingMode roundingMode) {
        MoneyUtils.checkNotNull(roundingMode, "RoundingMode must not be null");
        if (scale >= getScale()) {
            return this;
        }
        int currentScale = iAmount.scale();
        BigDecimal amount = iAmount.setScale(scale, roundingMode).setScale(currentScale);
        return BigMoney.of(iCurrency, amount);
    }

    //-----------------------------------------------------------------------
    /**
     * Returns a copy of this monetary value converted into another currency
     * using the specified conversion rate.
     * <p>
     * The scale of the result will be the sum of the scale of this money and
     * the scale of the multiplier. If desired, the scale of the result can be
     * adjusted to the scale of the new currency using {@link #withCurrencyScale()}.
     * <p>
     * This instance is immutable and unaffected by this method.
     * 
     * @param currency  the new currency, not null
     * @param conversionMultipler  the conversion factor between the currencies, not null
     * @return the new multiplied instance, never null
     * @throws MoneyException if the currency is the same as this currency
     * @throws MoneyException if the conversion multiplier is negative
     */
    public BigMoney convertedTo(CurrencyUnit currency, BigDecimal conversionMultipler) {
        MoneyUtils.checkNotNull(currency, "CurrencyUnit must not be null");
        MoneyUtils.checkNotNull(conversionMultipler, "Multiplier must not be null");
        if (currency == iCurrency) {
            throw new MoneyException("Cannot convert to the same currency");
        }
        if (conversionMultipler.compareTo(BigDecimal.ZERO) < 0) {
            throw new MoneyException("Cannot convert using a negative conversion multiplier");
        }
        BigDecimal amount = iAmount.multiply(conversionMultipler);
        return BigMoney.of(currency, amount);
    }

    /**
     * Returns a copy of this monetary value converted into another currency
     * using the specified conversion rate, with a rounding mode used to adjust
     * the decimal places in the result.
     * <p>
     * The result will have the same scale as this instance even though it will
     * be in a different currency.
     * <p>
     * This instance is immutable and unaffected by this method.
     * 
     * @param currency  the new currency, not null
     * @param conversionMultipler  the conversion factor between the currencies, not null
     * @param roundingMode  the rounding mode to use to bring the decimal places back in line, not null
     * @return the new multiplied instance, never null
     * @throws MoneyException if the currency is the same as this currency
     * @throws MoneyException if the conversion multiplier is negative
     * @throws ArithmeticException if the rounding fails
     */
    public BigMoney convertRetainScale(CurrencyUnit currency, BigDecimal conversionMultipler, RoundingMode roundingMode) {
        return convertedTo(currency, conversionMultipler).withScale(getScale(), roundingMode);
    }

    //-----------------------------------------------------------------------
    /**
     * Implements the {@code BigMoneyProvider} interface, trivially
     * returning {@code this}.
     * 
     * @return the money instance, never null
     */
    public BigMoney toBigMoney() {
        return this;
    }

    /**
     * Converts this money to an instance of {@code FixedMoney} with the same scale.
     * 
     * @return the money instance, never null
     */
    public FixedMoney toFixedMoney() {
        return FixedMoney.from(this);
    }

    /**
     * Converts this money to an instance of {@code Money} without rounding.
     * If the scale of this money exceeds the currency scale an exception will be thrown.
     * 
     * @return the money instance, never null
     * @throws ArithmeticException if the rounding fails
     */
    public Money toMoney() {
        return Money.from(this);
    }

    /**
     * Converts this money to an instance of {@code Money}.
     * 
     * @param roundingMode  the rounding mode to use, not null
     * @return the money instance, never null
     * @throws ArithmeticException if the rounding fails
     */
    public Money toMoney(RoundingMode roundingMode) {
        return Money.from(this, roundingMode);
    }

    //-----------------------------------------------------------------------
    /**
     * Checks if this instance and the specified instance have the same currency.
     * 
     * @param money  the money to check, not null
     * @return true if they have the same currency
     */
    public boolean isSameCurrency(BigMoneyProvider money) {
        return (iCurrency.equals(from(money).getCurrencyUnit()));
    }

    //-----------------------------------------------------------------------
    /**
     * Compares this monetary value to another.
     * The compared values must be in the same currency.
     * 
     * @param other  the other monetary value, not null
     * @return -1 if this is less than , 0 if equal, 1 if greater than
     * @throws MoneyException if the currencies differ
     */
    public int compareTo(BigMoneyProvider other) {
        BigMoney otherMoney = from(other);
        if (iCurrency.equals(otherMoney.iCurrency) == false) {
            throw new MoneyException("Cannot compare " + this + " to " + otherMoney + " as the currencies differ");
        }
        return iAmount.compareTo(otherMoney.iAmount);
    }

    /**
     * Checks if this monetary value is equal to another.
     * <p>
     * This ignores the scale of the amount.
     * Thus, 'USD 30.00' and 'USD 30' are equal.
     * <p>
     * The compared values must be in the same currency.
     * 
     * @param other  the other monetary value, not null
     * @return true is this is greater than the specified monetary value
     * @throws MoneyException if the currencies differ
     * @see #equals(Object)
     */
    public boolean isEqual(BigMoneyProvider other) {
        return compareTo(other) == 0;
    }

    /**
     * Checks if this monetary value is greater than another.
     * The compared values must be in the same currency.
     * 
     * @param other  the other monetary value, not null
     * @return true is this is greater than the specified monetary value
     * @throws MoneyException if the currencies differ
     */
    public boolean isGreaterThan(BigMoneyProvider other) {
        return compareTo(other) > 0;
    }

    /**
     * Checks if this monetary value is less than another.
     * The compared values must be in the same currency.
     * 
     * @param other  the other monetary value, not null
     * @return true is this is less than the specified monetary value
     * @throws MoneyException if the currencies differ
     */
    public boolean isLessThan(BigMoneyProvider other) {
        return compareTo(other) < 0;
    }

    //-----------------------------------------------------------------------
    /**
     * Checks if this monetary value equals another.
     * <p>
     * Like BigDecimal, this method compares the scale of the amount.
     * Thus, 'USD 30.00' and 'USD 30' are not equal.
     * <p>
     * The compared values must be in the same currency.
     * 
     * @param other  the other object, null returns false
     * @return true if this instance equals the other instance
     * @see #isEqual
     */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other instanceof BigMoney) {
            BigMoney otherMoney = (BigMoney) other;
            return iCurrency.equals(otherMoney.getCurrencyUnit()) &&
                    iAmount.equals(otherMoney.iAmount);
        }
        return false;
    }

    /**
     * Returns a hash code for this monetary value.
     * 
     * @return a suitable hash code
     */
    @Override
    public int hashCode() {
        return iCurrency.hashCode() ^ iAmount.hashCode();
    }

    //-----------------------------------------------------------------------
    /**
     * Gets this monetary value as a string.
     * <p>
     * The format is the 3 letter ISO currency code, followed by a space,
     * followed by the amount as per {@link BigDecimal#toPlainString()}.
     * 
     * @return the string representation of this monetary value, never null
     */
    @Override
    public String toString() {
        return new StringBuilder()
            .append(iCurrency.getCurrencyCode())
            .append(' ')
            .append(iAmount.toPlainString())
            .toString();
    }

}
