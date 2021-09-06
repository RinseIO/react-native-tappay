import { GetCardPrimeResolveValue } from "./card";

export interface SetupArgs {
  appId: number;
  appKey: string;
  env: string;
  rbaId?: string;
  rbaKey?: string;
}

type IsLoadedSuccess = boolean;

export type UseTapPay = [IsLoadedSuccess, TapPayInstance];

export default interface TapPayInstance {
  validateCard: (
    cardNumber: string,
    dueMonth: string,
    dueYear: string,
    CCV: string
  ) => Promise<any>;
  setCard: (
    cardNumber: string,
    dueMonth: string,
    dueYear: string,
    CCV: string
  ) => void;
  getDirectPayPrime: () => Promise<GetCardPrimeResolveValue>;
  removeCard: () => void;
  getLinePayPrime: (returnUrl: string) => Promise<{ prime: string | null }>;
  isLinePayAvailable: () => Promise<boolean>;
  handleLinePayURL: (url: string) => Promise<boolean>;
}