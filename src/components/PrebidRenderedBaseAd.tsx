import React from 'react';
import { requireNativeComponent, ViewProps } from 'react-native';

export type NativeEvent = {
  type: string;
  width?: number;
  height?: number;
  code?: number;
  message?: string;
};

export interface NativeProps extends ViewProps {
  configId?: string;
  adUnitId?: string;
  onNativeEvent?: (event: { nativeEvent: NativeEvent }) => void;
}

const COMPONENT_NAME = 'PrebidRenderedAdView';

const BaseAd = requireNativeComponent<NativeProps>(COMPONENT_NAME);

export default BaseAd;
