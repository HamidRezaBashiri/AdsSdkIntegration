import React from 'react';
import { requireNativeComponent, Platform } from 'react-native';

const InMobiBannerView = requireNativeComponent('InMobiBannerView');

const InMobiBanner = ({ placementId, style }) => {
  if (Platform.OS !== 'android') return null;
  
  return (
    <InMobiBannerView
      placementId={"10000060004"}
      style={style || { width: 320, height: 50 }}
    />
  );
};

export default InMobiBanner; 