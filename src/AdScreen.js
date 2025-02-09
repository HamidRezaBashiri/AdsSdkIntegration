import React, { useState, useRef, useEffect } from 'react';
import {
  View,
  Text,
  FlatList,
  TouchableOpacity,
  StyleSheet,
} from 'react-native';
import { BannerAd, BannerAdSize } from 'react-native-google-mobile-ads';
import PrebidAdView from './components/PrebidAdView';
import PrebidRenderedAdView from './components/PrebidRenderedAdView';

const PREBID_CONFIG_ID = 'prebid-demo-banner-320-50';
const PREBID_GAM_AD_UNIT_ID = '/23200903920/HCN/test_devteamBG_mpu4';
const GMA_BANNER_AD_UNIT_ID = '/23200903920/HCN/test_devteamBG_mpu4';
const INMOBI_BANNER_AD_UNIT_ID = '/23200903920/HCN/test_devteamBG_mpu3';
const FINAL_GAM_AD_UNIT_ID = '/23200903920/HCN/test_devteamBG_mpu3';

const adData = [
  { id: '1', sdk: 'GMA', adUnitId: GMA_BANNER_AD_UNIT_ID },
  { id: '2', sdk: 'InMobi', adUnitId: INMOBI_BANNER_AD_UNIT_ID },
  { id: '3', sdk: 'Prebid', configId: "prebid-demo-display-interstitial-320-480", adUnitId: PREBID_GAM_AD_UNIT_ID }, 
  { id: '4', sdk: 'PrebidRendered', configId: "prebid-demo-display-interstitial-320-480", adUnitId: PREBID_GAM_AD_UNIT_ID },
  { id: '5', sdk: 'FinalAuction', adUnitId: FINAL_GAM_AD_UNIT_ID, configId: PREBID_CONFIG_ID },
];

const AdScreen = () => {
  const [refreshKey, setRefreshKey] = useState(0);

  const loadAds = () => {
    console.log('Load Ads button clicked');
    setRefreshKey(prev => prev + 1);
    // You could also call a ref method on each PrebidAdView if you keep a separate ref or use a key-based approach.
  };

  const renderAd = ({ item }) => {
    switch (item.sdk) {
      case 'GMA':
        return (
          <View style={styles.adContainer}>
            <Text style={styles.sdkName}>GMA</Text>
            <BannerAd
              key={`gma-${refreshKey}`}
              unitId={item.adUnitId}
              size={BannerAdSize.BANNER}
              requestOptions={{ requestNonPersonalizedAdsOnly: true }}
              onAdLoaded={() => console.log('GMA Ad Loaded')}
              onAdFailedToLoad={(error) => console.error('GMA Ad Failed to Load:', error.message)}
            />
          </View>
        );
      case 'InMobi':
        return (
          <View style={styles.adContainer}>
            <Text style={styles.sdkName}>InMobi</Text>
            <BannerAd
              key={`inmobi-${refreshKey}`}
              unitId={item.adUnitId}
              size={BannerAdSize.BANNER}
              requestOptions={{ requestNonPersonalizedAdsOnly: true }}
              onAdLoaded={() => console.log('InMobi Ad Loaded')}
              onAdFailedToLoad={(error) => console.error('InMobi Ad Failed to Load:', error.message)}
            />
          </View>
        );
      case 'Prebid':
        return (
          <View style={styles.adContainer}>
            <Text style={styles.sdkName}>Prebid</Text>
            <PrebidAdView
              key={`prebid-${refreshKey}`}
              style={{ width: 320, height: 50 }}
              configId={item.configId}
              adUnitId={item.adUnitId}
              onAdLoaded={() => console.log('Prebid Ad Loaded')}
              onAdFailedToLoad={(error) =>
                console.error('Prebid Ad Failed to Load:', error)
              }
            />
          </View>
        );
        case 'PrebidRendered':
          return (
            <View style={styles.adContainer}>
              <Text style={styles.sdkName}>Prebid Rendered</Text>
              <PrebidRenderedAdView
                key={`prebid-${refreshKey}`}
                style={{ width: 320, height: 50 }}
                configId={item.configId}
                adUnitId={item.adUnitId}
                onAdLoaded={() => console.log('PrebidRendered Ad Loaded')}
                onAdFailedToLoad={(error) =>
                  console.error('PrebidRendered Ad Failed to Load:', error)
                }
              />
            </View>
          );
  
      case 'FinalAuction': // New Auction Slot
        return (
          <View style={styles.adContainer}>
            <Text style={styles.sdkName}>Final GAM Auction</Text>
            <PrebidRenderedAdView
              key={`final-gam-${refreshKey}`}
              style={{ width: 320, height: 50 }}
              configId={item.configId}
              adUnitId={item.adUnitId}
              onAdLoaded={() => console.log('Final Auction Ad Loaded')}
              onAdFailedToLoad={(error) =>
                console.error('Final Auction Ad Failed to Load:', error)
              }
            />
          </View>
        );
      default:
        return (
          <View style={styles.adContainer}>
            <Text style={styles.sdkName}>Unsupported SDK</Text>
          </View>
        );
    }
  };

  return (
    <View style={styles.container}>
      <Text style={styles.title}>Ad Integration Demo</Text>
      <TouchableOpacity style={styles.button} onPress={loadAds}>
        <Text style={styles.buttonText}>Load Ads</Text>
      </TouchableOpacity>
      <FlatList
        data={adData}
        renderItem={renderAd}
        keyExtractor={(item) => item.id}
        contentContainerStyle={styles.listContainer}
        ListEmptyComponent={<Text style={styles.emptyText}>No ads loaded.</Text>}
      />
    </View>
  );
};

export default AdScreen;

const styles = StyleSheet.create({
  container: {
    flex: 1,
    padding: 16,
    backgroundColor: '#f5f5f5',
  },
  title: {
    fontSize: 20,
    fontWeight: 'bold',
    textAlign: 'center',
    marginBottom: 16,
  },
  button: {
    backgroundColor: '#007BFF',
    padding: 12,
    borderRadius: 8,
    alignItems: 'center',
    marginBottom: 16,
  },
  buttonText: {
    color: '#fff',
    fontSize: 16,
    fontWeight: 'bold',
  },
  listContainer: {
    paddingVertical: 16,
  },
  adContainer: {
    padding: 16,
    backgroundColor: '#fff',
    borderRadius: 8,
    marginBottom: 12,
    shadowColor: '#000',
    shadowOpacity: 0.1,
    shadowRadius: 6,
    elevation: 2,
    alignItems: 'center',
  },
  sdkName: {
    fontSize: 16,
    fontWeight: 'bold',
  },
  emptyText: {
    textAlign: 'center',
    fontSize: 16,
    color: '#aaa',
  },
});
