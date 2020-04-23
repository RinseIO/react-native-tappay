//
//  TapPay.swift
//  DirectPay
//
//  Created by xenos xavier on 2020/2/2.
//  Copyright © 2020 Facebook. All rights reserved.
//

import Foundation
import AdSupport

@objc(TapPay)
class TapPay: NSObject {
    private let cardTypes = [
        CardType.visa: 1,
        CardType.masterCard: 2,
        CardType.JCB: 3,
        CardType.unionPay: 4,
        CardType.AMEX: 5,
        CardType.unknown: -1,
    ]
    
    private var tpdCard: TPDCard?
    
    @objc
    func setup(_ appId: NSNumber, appKey: NSString, serverType: NSString) {
        let serverType: TPDServerType = (serverType == "production") ? .production : .sandBox
        TPDSetup.setWithAppId(appId.int32Value, withAppKey: appKey as String, with: serverType)
        let IDFA = ASIdentifierManager.shared().advertisingIdentifier.uuidString
        TPDSetup.shareInstance().setupIDFA(IDFA)
        TPDSetup.shareInstance().serverSync()
    }
    
    @objc
    func validateCard(
        _ cardNumber: String,
        withDueMonth dueMonth: String,
        withDueYear dueYear: String,
        withCCV ccv: String,
        resolver resolve: @escaping RCTPromiseResolveBlock,
        rejecter reject: @escaping RCTPromiseRejectBlock
    ) {
        let response = TPDCard.validate(withCardNumber: cardNumber, withDueMonth: dueMonth, withDueYear: dueYear, withCCV: ccv)
        
        if let result = response {
            resolve([
                "isCardNumberValid": result.isCardNumberValid,
                "isExpiredDateValid": result.isExpiryDateValid,
                "isCCVValid": result.isCCVValid,
                "cardType": self.cardTypes[result.cardType] ?? self.cardTypes[CardType.unknown]!
            ])
        } else {
            reject("Error Code", "No Response", nil)
        }
    }
    
    @objc
    func getDirectPayPrime(
        _ cardNumber: String,
        withDueMonth dueMonth: String,
        withDueYear dueYear: String,
        withCCV ccv: String,
        resolver resolve: @escaping RCTPromiseResolveBlock,
        rejecter reject: @escaping RCTPromiseRejectBlock
    ) {
        let tpdCard = TPDCard.setWithCardNumber(cardNumber, withDueMonth: dueMonth, withDueYear: dueYear, withCCV: ccv)
        
        tpdCard.onSuccessCallback { (prime, cardInfo, cardIdentifier) in
            if
                let directPayPrime = prime, directPayPrime != "",
                let creditCardInfo = cardInfo,
                let creditCardIdentifier = cardIdentifier
            {
                resolve([
                    "prime": directPayPrime,
                    "binCode": creditCardInfo.bincode ?? "",
                    "lastFour": creditCardInfo.lastFour ?? "",
                    "issuer": creditCardInfo.issuer ?? "",
                    "cardType": creditCardInfo.cardType,
                    "funding": creditCardInfo.funding,
                    "cardIdentifier": creditCardIdentifier
                ])
            }
        }.onFailureCallback { (status, message) in
            reject(String(status), message, nil)
        }.createToken(withGeoLocation: "UNKNOWN")
    }
    
    @objc
    func setCard(
        _ cardNumber: String,
        withDueMonth dueMonth: String,
        withDueYear dueYear: String,
        withCCV ccv: String
    ) {
        self.tpdCard = TPDCard.setWithCardNumber(cardNumber, withDueMonth: dueMonth, withDueYear: dueYear, withCCV: ccv)
    }
    
    @objc
    func removeCard() {
        self.tpdCard = nil
    }
    
    @objc
    func getDirectPayPrime(
        _ resolve: @escaping RCTPromiseResolveBlock,
        rejecter reject: @escaping RCTPromiseRejectBlock
    ) {
        
        if let tpdCard = self.tpdCard {
            tpdCard.onSuccessCallback { (prime, cardInfo, cardIdentifier) in
                if
                    let directPayPrime = prime, directPayPrime != "",
                    let creditCardInfo = cardInfo,
                    let creditCardIdentifier = cardIdentifier
                {
                    resolve([
                        "prime": directPayPrime,
                        "binCode": creditCardInfo.bincode ?? "",
                        "lastFour": creditCardInfo.lastFour ?? "",
                        "issuer": creditCardInfo.issuer ?? "",
                        "cardType": creditCardInfo.cardType,
                        "funding": creditCardInfo.funding,
                        "cardIdentifier": creditCardIdentifier
                    ])
                }
            }.onFailureCallback { (status, message) in
                reject(String(status), message, nil)
            }.createToken(withGeoLocation: "UNKNOWN")
        }
    }
}