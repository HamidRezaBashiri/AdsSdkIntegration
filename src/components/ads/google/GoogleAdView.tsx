import React from 'react';
import { 
  requireNativeComponent, 
  ViewStyle, 
  NativeSyntheticEvent,
  StyleProp 
} from 'react-native';

// Separate interfaces for props
interface GoogleAdViewProps {
  adUnitId: string;
  style?: StyleProp<ViewStyle>;
  onAdLoaded?: () => void;
  onAdFailedToLoad?: (event: AdLoadErrorEvent) => void;
}

// Interface for native events
interface AdLoadErrorEvent {
  error: string;
  code?: number;
  domain?: string;
}

// Native component doesn't extend GoogleAdViewProps
interface NativeGoogleAdViewProps {
  adUnitId: string;
  style?: StyleProp<ViewStyle>;
  onAdLoaded?: () => void;
  onAdFailedToLoad?: (event: NativeSyntheticEvent<AdLoadErrorEvent>) => void;
}

const NativeGoogleAdView = requireNativeComponent<NativeGoogleAdViewProps>('RNGoogleAdView');

const GoogleAdView: React.FC<GoogleAdViewProps> = ({
  adUnitId,
  style,
  onAdLoaded,
  onAdFailedToLoad
}) => {
  const handleAdLoaded = () => {
    onAdLoaded?.();
  };

  const handleAdFailedToLoad = (event: NativeSyntheticEvent<AdLoadErrorEvent>) => {
    onAdFailedToLoad?.(event.nativeEvent);
  };

  return (
    <NativeGoogleAdView
      adUnitId={adUnitId}
      style={[{ width: '100%', height: 50 }, style]}
      onAdLoaded={handleAdLoaded}
      onAdFailedToLoad={handleAdFailedToLoad}
    />
  );
};

export default GoogleAdView;