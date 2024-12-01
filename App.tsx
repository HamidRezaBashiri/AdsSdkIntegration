import React, { useState, useCallback } from 'react';
import { View, Button, StyleSheet, Text } from 'react-native';
import GoogleAdView from './src/components/ads/google/GoogleAdView';
import { GoogleAdSDK } from './src/components/ads/google/GoogleAdSDK';

const App = () => {
  const [showAd, setShowAd] = useState(false);
  const [adStatus, setAdStatus] = useState<'idle' | 'loading' | 'loaded' | 'error'>('idle');

  const handleShowAd = async () => {
    try {
      setAdStatus('loading');
      
      // Initialize SDK if not already initialized
      const isInitialized = await GoogleAdSDK.isInitialized();
      if (!isInitialized) {
        await GoogleAdSDK.initialize();
      }

      // Load the ad
      const result = await GoogleAdSDK.loadAd('ca-app-pub-3940256099942544/6300978111');
      
      if (result.status === 'success') {
        setShowAd(true);
        setAdStatus('loaded');
      } else {
        setAdStatus('error');
      }
    } catch (error) {
      console.error('Ad loading failed:', error);
      setAdStatus('error');
    }
  };

  const handleAdLoaded = useCallback(() => {
    console.log('Ad loaded successfully');
    setAdStatus('loaded');
  }, []);

  const handleAdFailedToLoad = useCallback((event: { error: string }) => {
    console.error('Ad failed to load:', event.error);
    setAdStatus('error');
  }, []);

  return (
    <View style={styles.container}>
      <Button 
        title="Show Ad" 
        onPress={handleShowAd}
        disabled={adStatus === 'loading'} 
      />
      
      {adStatus === 'loading' && <Text>Loading ad...</Text>}
      
      {showAd && (
        <GoogleAdView
          adUnitId="ca-app-pub-3940256099942544/6300978111"
          style={styles.adContainer}
          onAdLoaded={handleAdLoaded}
          onAdFailedToLoad={handleAdFailedToLoad}
        />
      )}
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    padding: 16,
  },
  adContainer: {
    width: '100%',
    height: 50,
    marginTop: 20,
  },
});

export default App;