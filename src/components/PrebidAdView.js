import React from 'react';
import { requireNativeComponent, View, StyleSheet } from 'react-native';

// Require the native Prebid view
const PrebidAdViewNative = requireNativeComponent('PrebidAdView');

const PrebidAdView = ({ configId, adUnitId, width = 320, height = 50 }) => {
  return (
    <View style={[styles.container, { width, height }]}>
      <PrebidAdViewNative
        style={{ width, height }}
        configId={configId}
        adUnitId={adUnitId}
        width={width}
        height={height}
      />
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    alignItems: 'center',
    justifyContent: 'center',
  },
});

export default PrebidAdView;
