package tw.com.verybuy.tappay;

import android.preference.PreferenceManager;

import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.uimanager.IllegalViewOperationException;

import java.util.HashMap;

import tech.cherri.tpdirect.api.TPDCard;

import tech.cherri.tpdirect.api.TPDGooglePay;
import tech.cherri.tpdirect.callback.TPDGooglePayListener;
import tech.cherri.tpdirect.callback.dto.TPDCardInfoDto;
import tech.cherri.tpdirect.callback.TPDCardGetPrimeSuccessCallback;
import tech.cherri.tpdirect.callback.TPDGetPrimeFailureCallback;
import tech.cherri.tpdirect.callback.dto.TPDMerchantReferenceInfoDto;
import tech.cherri.tpdirect.callback.TPDLinePayGetPrimeSuccessCallback;

import tech.cherri.tpdirect.api.TPDCardValidationResult;
import tech.cherri.tpdirect.api.TPDLinePayResult;
import tech.cherri.tpdirect.api.TPDSetup;
import tech.cherri.tpdirect.api.TPDLinePay;
import tech.cherri.tpdirect.api.TPDServerType;
import tech.cherri.tpdirect.callback.TPDLinePayResultListener;
import tech.cherri.tpdirect.exception.TPDLinePayException;
import tech.cherri.tpdirect.api.TPDConsumer;
import tech.cherri.tpdirect.api.TPDGooglePay;
import tech.cherri.tpdirect.api.TPDMerchant;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.wallet.AutoResolveHelper;
import com.google.android.gms.wallet.PaymentData;
import com.google.android.gms.wallet.TransactionInfo;
import com.google.android.gms.wallet.WalletConstants;

import android.app.Activity;
import android.content.Intent;

public class TapPayModule extends ReactContextBaseJavaModule implements ActivityEventListener {

    private final ReactApplicationContext reactContext;
    private TPDCard tpdCard;
    private String cardNumber;
    private String dueMonth;
    private String dueYear;
    private String CCV;
    private TPDLinePay tpdLinePay;
    private TPDCard.CardType[] allowedNetworks = new TPDCard.CardType[]{TPDCard.CardType.Visa
            , TPDCard.CardType.MasterCard
            , TPDCard.CardType.JCB
            , TPDCard.CardType.AmericanExpress};
    private TPDCard.AuthMethod[] allowedAuthMethods = new TPDCard.AuthMethod[]{TPDCard.AuthMethod.Cryptogram3DS};
    private static final int REQUEST_READ_PHONE_STATE = 101;
    private static final int LOAD_PAYMENT_DATA_REQUEST_CODE = 102;
    private TPDGooglePay tpdGooglePay;
    private PaymentData paymentData;


    private final HashMap<TPDCard.CardType, Integer> cardTypes = new HashMap<TPDCard.CardType, Integer>() {{
        put(TPDCard.CardType.Unknown, -1);
        put(TPDCard.CardType.Visa, 1);
        put(TPDCard.CardType.MasterCard, 2);
        put(TPDCard.CardType.JCB, 3);
        put(TPDCard.CardType.UnionPay, 4);
        put(TPDCard.CardType.AmericanExpress, 5);
    }};

    public TapPayModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
    }

    @Override
    public String getName() {
        return "TapPay";
    }

    @ReactMethod
    public void setup(int appID, String appKey, String serverTypeString) {
        TPDServerType serverType = serverTypeString.equals("production") ? TPDServerType.Production : TPDServerType.Sandbox;

        TPDSetup.initInstance(this.reactContext,
                appID,
                appKey,
                serverType);
    }

    @ReactMethod
    public void setupWithRBA(int appID, String appKey, String rbaId, String rbaKey, String serverTypeString) {
        TPDServerType serverType = serverTypeString.equals("production") ? TPDServerType.Production : TPDServerType.Sandbox;

        TPDSetup.initInstanceWithRba(this.reactContext,
               appID,
               appKey,
               rbaId,
               rbaKey,
               serverType);
    }

    @ReactMethod
    public void validateCard(String cardNumber, String dueMonth, String dueYear, String CCV, Promise promise) {

        try {

            TPDCardValidationResult result = TPDCard.validate(
                    new StringBuffer(cardNumber),
                    new StringBuffer(dueMonth),
                    new StringBuffer(dueYear),
                    new StringBuffer(CCV));

            WritableMap map = Arguments.createMap();
            map.putBoolean("isCardNumberValid", result.isCardNumberValid());
            map.putBoolean("isExpiredDateValid", result.isExpiryDateValid());
            map.putBoolean("isCCVValid", result.isCCVValid());
            map.putInt("cardType", this.cardTypes.get(result.getCardType()));

            promise.resolve(map);

        } catch (IllegalViewOperationException e) {
            promise.reject("TPDCard.validate Error", e);
        }


    }

    @ReactMethod
    public void setCard(String cardNumber, String dueMonth, String dueYear, String CCV, Promise promise) {
        this.cardNumber = cardNumber;
        this.dueMonth = dueMonth;
        this.dueYear = dueYear;
        this.CCV = CCV;

        this.tpdCard = new TPDCard(this.reactContext,
                new StringBuffer(cardNumber),
                new StringBuffer(dueMonth),
                new StringBuffer(dueYear),
                new StringBuffer(CCV));


    }

    @ReactMethod
    public void removeCard() {
        this.tpdCard = null;
        this.cardNumber = null;
        this.dueMonth = null;
        this.dueYear = null;
        this.CCV = null;
    }

    @ReactMethod
    public void getDirectPayPrime(final Promise promise) {
        if (null != this.tpdCard) {
            try {
                this.tpdCard = new TPDCard(this.reactContext,
                        new StringBuffer(this.cardNumber),
                        new StringBuffer(this.dueMonth),
                        new StringBuffer(this.dueYear),
                        new StringBuffer(this.CCV));

                this.tpdCard.onSuccessCallback(new TPDCardGetPrimeSuccessCallback() {
                    @Override
                    public void onSuccess(String token, TPDCardInfoDto tpdCardInfo, String cardIdentifier, TPDMerchantReferenceInfoDto merchantReferenceInfo) {
                        String cardLastFour = tpdCardInfo.getLastFour();
                        WritableMap map = Arguments.createMap();

                        map.putString("prime", token);
                        map.putString("bincode", tpdCardInfo.getBincode());
                        map.putString("lastfour", tpdCardInfo.getLastFour());
                        map.putString("issuer", tpdCardInfo.getIssuer());
                        map.putInt("type", tpdCardInfo.getCardType());
                        map.putInt("funding", tpdCardInfo.getFunding());
                        map.putString("cardidentifier", cardIdentifier);
                        map.putString("merchantReferenceInfo", String.valueOf(merchantReferenceInfo));
                        promise.resolve(map);
                    }
                }).onFailureCallback(new TPDGetPrimeFailureCallback() {
                    @Override
                    public void onFailure(int status, String reportMsg) {
                        //Failure
                        promise.reject(String.valueOf(status), reportMsg);
                    }
                }).createToken("UNKNOWN");
            } catch (IllegalViewOperationException e) {
                promise.reject("TPDCard.createToken Error", e);
            }
        }else {
            promise.reject("-1", "TPDCard is null");
        }
    }

    @ReactMethod
    public void isLinePayAvailable(final Promise promise) throws TPDLinePayException {
        promise.resolve(TPDLinePay.isLinePayAvailable(this.reactContext));
    }

    @ReactMethod
    public void getLinePayPrime(String returnUrl, final Promise promise) throws TPDLinePayException {
        boolean isLinePayAvailable = TPDLinePay.isLinePayAvailable(this.reactContext);
        if (isLinePayAvailable) {
            try {
                tpdLinePay = new TPDLinePay(this.reactContext, returnUrl);

                tpdLinePay.getPrime(new TPDLinePayGetPrimeSuccessCallback() {
                    @Override
                    public void onSuccess(String prime) {
                        WritableMap map = Arguments.createMap();
                        map.putString("prime", prime);

                        promise.resolve(map);
                    }
                }, new TPDGetPrimeFailureCallback () {
                    @Override
                    public void onFailure(int status, String reportMsg) {
                        promise.reject(String.valueOf(status), reportMsg);
                    }
                });
            } catch (TPDLinePayException e) {
                promise.reject("Fail", e.getMessage());
            }
        } else {
            promise.reject("Fail", "LINE pay is not exist.");
        }
    }

    @ReactMethod
    public void handleLinePayURL(String url, final Promise promise) throws TPDLinePayException {
        tpdLinePay.parseToLinePayResult(this.reactContext, this.reactContext.getCurrentActivity().getIntent().getData(), new TPDLinePayResultListener() {
            @Override
            public void onParseSuccess(TPDLinePayResult tpdLinePayResult) {
                if (tpdLinePayResult != null && tpdLinePayResult.getStatus() == 0) {
                    promise.resolve(true);
                } else {
                    promise.reject("Fail", "LINE pay transaction is fail.");
                }
            }

            @Override
            public void onParseFail(int status, String message) {
                promise.reject("Fail", message);
            }
        });
    }

    @ReactMethod
    public void getGooglePayPrime(final Promise promise) {
        TPDMerchant tpdMerchant = new TPDMerchant();
        tpdMerchant.setSupportedNetworks(allowedNetworks);
        tpdMerchant.setMerchantName(Constants.TEST_MERCHANT_NAME);
        tpdMerchant.setSupportedAuthMethods(allowedAuthMethods);

        TPDConsumer tpdConsumer = new TPDConsumer();
        tpdConsumer.setPhoneNumberRequired(false);
        tpdConsumer.setShippingAddressRequired(false);
        tpdConsumer.setEmailRequired(false);


        tpdGooglePay = new TPDGooglePay(this.reactContext.getCurrentActivity(), tpdMerchant, tpdConsumer);
        tpdGooglePay.isGooglePayAvailable(new TPDGooglePayListener() {
            @Override
            public void onReadyToPayChecked(boolean b, String s) {
                if (b) {
                    tpdGooglePay.requestPayment(TransactionInfo.newBuilder()
                            .setTotalPriceStatus(WalletConstants.TOTAL_PRICE_STATUS_NOT_CURRENTLY_KNOWN)
                            .setCurrencyCode("TWD")
                            .build(), LOAD_PAYMENT_DATA_REQUEST_CODE);
                    tpdGooglePay.getPrime();

                }
            }
        });

    }
    @Override
    public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case LOAD_PAYMENT_DATA_REQUEST_CODE:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        confirmBTN.setEnabled(true);
                        paymentData = PaymentData.getFromIntent(data);
                        revealPaymentInfo(paymentData);
                        break;
                    case Activity.RESULT_CANCELED:
                        confirmBTN.setEnabled(false);
                        showMessage("Canceled by User");
                        break;
                    case AutoResolveHelper.RESULT_ERROR:
                        confirmBTN.setEnabled(false);
                        Status status = AutoResolveHelper.getStatusFromIntent(data);
                        Log.d(TAG, "AutoResolveHelper.RESULT_ERROR : " + status.getStatusCode() + " , message = " + status.getStatusMessage());
                        showMessage(status.getStatusCode() + " , message = " + status.getStatusMessage());
                        break;
                    default:
                        // Do nothing.
                }
                break;
            default:
                // Do nothing.
        }
    }

    @ReactMethod
    public void isApplePayAvailable(final Promise promise) {
        promise.resolve(false);
    }

    @ReactMethod
    public void getApplePayPrime(final Promise promise) {
        promise.reject("Fail", "Not Support on Android");
    }
}
