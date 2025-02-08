import React, { forwardRef } from 'react';
import { PixelRatio } from 'react-native';
import BaseAd from './PrebidRenderedBaseAd';
import { NativeEvent } from './PrebidRenderedBaseAd';
import { UIManager, findNodeHandle } from 'react-native';

// If you want to keep codegenNativeCommands, do so. Otherwise you can also do
// direct UIManager.dispatchViewManagerCommand approach:
type NativeCommands = {
  load: (viewRef: any) => void;
};

export const PrebidAdCommands: NativeCommands = {
  load(viewRef: any) {
    UIManager.dispatchViewManagerCommand(
      findNodeHandle(viewRef),
      UIManager.getViewManagerConfig('PrebidRenderedAdView').Commands.load,
      []
    );
  },
};

type PrebidAdViewProps = {
  configId?: string;
  adUnitId?: string;
  style?: any;
  onAdLoaded?: (dims: { width: number; height: number }) => void;
  onAdFailedToLoad?: (error: { code: number; message: string }) => void;
};

const PrebidAdView = forwardRef<any, PrebidAdViewProps>((props, ref) => {
  const nativeRef = React.useRef<any>(null);

  React.useImperativeHandle(ref, () => ({
    load: () => {
      if (nativeRef.current) {
        PrebidAdCommands.load(nativeRef.current);
      }
    },
  }));

  const handleNativeEvent = (event: { nativeEvent: NativeEvent }) => {
    const { type, width, height, code, message } = event.nativeEvent;
    switch (type) {
      case 'loaded':
        console.log('PrebidRenderedAdView loaded');
        if (props.onAdLoaded && width && height) {
          props.onAdLoaded({
            width: PixelRatio.getPixelSizeForLayoutSize(width),
            height: PixelRatio.getPixelSizeForLayoutSize(height),
          });
        }
        break;
      case 'error':
        if (props.onAdFailedToLoad && code && message) {
          props.onAdFailedToLoad({ code, message });
        }
        break;
    }
  };

  return (
    <BaseAd
      ref={nativeRef}
      configId={props.configId}
      adUnitId={props.adUnitId}
      onNativeEvent={handleNativeEvent}
      style={props.style}
    />
  );
});

export default React.memo(PrebidAdView);
