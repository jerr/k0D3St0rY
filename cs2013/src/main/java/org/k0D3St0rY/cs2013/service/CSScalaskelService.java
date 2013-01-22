package org.k0D3St0rY.cs2013.service;

import java.util.List;
import java.util.Map;

import org.jboss.netty.logging.InternalLogger;
import org.jboss.netty.logging.InternalLoggerFactory;

public class CSScalaskelService extends AbstractCSService {

    static final InternalLogger logger = InternalLoggerFactory.getInstance(CSScalaskelService.class);

    final int change;
    String json;

    public CSScalaskelService(int change) {
        this.change = change;
    }

    @Override
    public CharSequence execute(Map<String, List<String>> params) {
        if (json == null)
            compile();
        return json;
    }

    public CSScalaskelService compile() {
        if(json == null)
            json = (new Wallet(change, compile(Currency.BAZ, change))).toJSON();
        return this;
    }

    private CurrencyWallet[] compile(Currency currency, int change) {
        int max = currency.divide(change);
        if (currency.before == null) {
            return new CurrencyWallet[] { new CurrencyWallet(currency, max, null) };
        } else {
            CurrencyWallet[] wallets = new CurrencyWallet[max + 1];
            for (int i = 0; i <= max; i++) {
                wallets[i] = new CurrencyWallet(currency, i, compile(currency.before, (change - currency.multiply(i))));
            }
            return wallets;
        }
    }
    

    static enum Currency {

        FOO(1, null), BAR(7, FOO), QIX(11, BAR), BAZ(21, QIX);

        final int value;
        final Currency before;
        Currency next;

        private Currency(int val, Currency before) {
            this.value = val;
            this.before = before;
            if (before != null)
                before.next = this;
        }

        int divide(int val) {
            return (val >= 0 ? val / value : 0);
        }

        int multiply(int val) {
            return value * val;
        }
    }

    static class Wallet {
        final private CurrencyWallet[] subWallet;
        final private int value;

        public Wallet(int value, CurrencyWallet... subWallet) {
            this.subWallet = subWallet;
            this.value = value;
        }

        public String toJSON() {
            StringBuilder builder = null;
            char comma = ',';
            for (Wallet walletBaz : this.subWallet) {
                for (Wallet walletQix : walletBaz.subWallet) {
                    for (Wallet walletBar : walletQix.subWallet) {
                        for (Wallet walletFoo : walletBar.subWallet) {
                            if (builder == null)
                                builder = new StringBuilder("[{");
                            else
                                builder.append(",{");
                            int count = 0;
                            if (count < (count =+ walletFoo.value())) {
                                builder.append(walletFoo.toJSON());
                                if (count != value)
                                    builder.append(comma);
                            }
                            if (count < (count =+ walletBar.value())) {
                                builder.append(walletBar.toJSON());
                                if (count != value)
                                    builder.append(comma);
                            }
                            if (count < (count =+ walletQix.value())) {
                                builder.append(walletQix.toJSON());
                                if (count != value)
                                    builder.append(comma);
                            }
                            if (count < (count =+ walletBaz.value()))
                                builder.append(walletBaz.toJSON());
                            builder.append('}');
                            if (count != value) {
                                logger.error(" value=" + value + " " + walletFoo.toJSON() + " " + walletBar.toJSON() + " "
                                        + walletQix.toJSON() + " " + walletBaz.toJSON());
                            }
                        }
                    }
                }
            }
            builder.append("]");
            return builder.toString();
        }

        public int value() {
            return value;
        }

    }

    static class CurrencyWallet extends Wallet {
        final private Currency currency;
        final private int nb;

        public CurrencyWallet(Currency currency, int nb, CurrencyWallet... subWallet) {
            super(currency.multiply(nb) + ((subWallet != null && subWallet.length > 0) ? subWallet[0].value() : 0), subWallet);
            this.currency = currency;
            this.nb = nb;
        }
        public String toJSON() {
            return "\"" + currency.name().toLowerCase() + "\": " + nb;
        }

    }
}
