import React, { useState, useEffect, useRef } from 'react';
import {
  View,
  Text,
  FlatList,
  TouchableOpacity,
  StyleSheet,
} from 'react-native';
import { BannerAd, BannerAdSize } from 'react-native-google-mobile-ads';
import { requireNativeComponent } from 'react-native';

const PrebidAdViewNative = requireNativeComponent('PrebidAdView');

function PrebidAdView({ configId, adUnitId, width = 320, height = 50, refreshKey }) {
  return (
    <View style={[styles.adWrapper, { width, height }]}>
      <PrebidAdViewNative
        style={{ width, height }}
        configId={configId}
        adUnitId={adUnitId}
        width={width}
        height={height}
        key={`prebid-native-${refreshKey}`}
      />
    </View>
  );
}

const PREBID_CONFIG_ID = 'CONFIG ID';
const PREBID_GAM_AD_UNIT_ID = '/23200903920/HCN/test_devteamBG_mpu4';

const GMA_BANNER_AD_UNIT_ID = __DEV__
  ? '/23200903920/HCN/test_devteamBG_mpu4'
  : '/23200903920/HCN/test_devteamBG_mpu3';

const INMOBI_BANNER_AD_UNIT_ID = __DEV__
  ? '/23200903920/HCN/test_devteamBG_mpu3'
  : '/23200903920/testmme';

const placeholderAds = [
  { id: '2', sdk: 'InMobi', ad: 'InMobi Banner Ad' },
  { id: '3', sdk: 'Prebid', ad: 'Prebid Banner Ad' },
  { id: '4', sdk: 'GAM', ad: 'Placeholder GAM Ad' },
];

const AdScreen = () => {
  const [ads, setAds] = useState([]);
  const [refreshKey, setRefreshKey] = useState(0);
  const refreshTimerRef = useRef(null);

  // Cleanup timer on unmount
  useEffect(() => {
    return () => {
      if (refreshTimerRef.current) {
        clearTimeout(refreshTimerRef.current);
      }
    };
  }, []);

  // Refresh ads every 120s
  useEffect(() => {
    if (ads.length > 0) {
      refreshTimerRef.current = setTimeout(() => {
        console.log('Refreshing ads - 120s mark');
        setRefreshKey((prev) => prev + 1);
      }, 120000);
    }
    return () => {
      if (refreshTimerRef.current) {
        clearTimeout(refreshTimerRef.current);
      }
    };
  }, [ads]);

  const loadAds = () => {
    setAds([{ id: '1', sdk: 'GMA', ad: 'Banner Ad' }, ...placeholderAds]);
    setRefreshKey((prev) => prev + 1);
  };

  const renderItem = ({ item }) => {
    // 1) GMA
    if (item.sdk === 'GMA') {
      return (
        <View style={styles.adContainer}>
          <Text style={styles.sdkName}>GMA</Text>
          <View style={styles.adWrapper}>
            <BannerAd
              key={`gma-${refreshKey}`}
              unitId={GMA_BANNER_AD_UNIT_ID}
              size={BannerAdSize.BANNER}
              requestOptions={{ requestNonPersonalizedAdsOnly: true }}
              onAdFailedToLoad={(error) => console.log('GMA Ad Load Failed:', error.message)}
              onAdLoaded={(adInfo) => {
                console.log('GMA Ad Loaded Successfully');
                console.log('Full Ad Info:', JSON.stringify(adInfo));
              }}
            />
          </View>
        </View>
      );
    }

    // 2) InMobi
    if (item.sdk === 'InMobi') {
      return (
        <View style={styles.adContainer}>
          <Text style={styles.sdkName}>InMobi</Text>
          <View style={styles.adWrapper}>
            <BannerAd
              key={`inmobi-${refreshKey}`}
              unitId={INMOBI_BANNER_AD_UNIT_ID}
              size={BannerAdSize.BANNER}
              requestOptions={{ requestNonPersonalizedAdsOnly: true }}
              onAdFailedToLoad={(error) => console.log('InMobi Ad Load Failed:', error.message)}
              onAdLoaded={() => console.log('InMobi Ad Loaded Successfully')}
            />
          </View>
        </View>
      );
    }

    // 3) Prebid
    if (item.sdk === 'Prebid') {
      return (
        <View style={styles.adContainer}>
        <Text style={styles.sdkName}>Prebid</Text>
        {/* Removed the red background */}
        <PrebidAdView
          configId={PREBID_CONFIG_ID}
          adUnitId={PREBID_GAM_AD_UNIT_ID}
          width={320}
          height={50}
          refreshKey={refreshKey}
        />
      </View>
      );
    }

    // 4) Another GAM placeholder
    if (item.sdk === 'GAM') {
      return (
        <View style={styles.adContainer}>
          <Text style={styles.sdkName}>Another GAM Auction</Text>
          <Text style={styles.adContent}>{item.ad}</Text>
        </View>
      );
    }

    // Fallback
    return (
      <View style={styles.adContainer}>
        <Text style={styles.sdkName}>{item.sdk}</Text>
        <Text style={styles.adContent}>{item.ad}</Text>
      </View>
    );
  };

  return (
    <View style={styles.container}>
      <Text style={styles.title}>Ad Integration Demo</Text>
      <TouchableOpacity style={styles.button} onPress={loadAds}>
        <Text style={styles.buttonText}>Load Ads</Text>
      </TouchableOpacity>

      <FlatList
        data={ads}
        renderItem={renderItem}
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
  },
  sdkName: {
    fontSize: 16,
    fontWeight: 'bold',
    marginBottom: 8,
  },
  adWrapper: {
    alignItems: 'center',
    justifyContent: 'center',
    marginTop: 8,
  },
  adContent: {
    fontSize: 14,
    color: '#555',
  },
  emptyText: {
    textAlign: 'center',
    fontSize: 16,
    color: '#aaa',
  },
});
