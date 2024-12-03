import React, { useState, useEffect } from 'react';
import {
  View,
  Text,
  FlatList,
  TouchableOpacity,
  StyleSheet,
} from 'react-native';
import {
  BannerAd,
  BannerAdSize,
  TestIds,
  MobileAds,
} from 'react-native-google-mobile-ads';

// Use the test Ad Unit ID for development
const GMA_BANNER_AD_UNIT_ID = __DEV__ 
  ? TestIds.BANNER 
  : 'ca-app-pub-3940256099942544/6300978111'; // Replace with your actual ad unit ID

const placeholderAds = [
  { id: '2', sdk: 'InMobi', ad: 'Placeholder for InMobi Ad' },
  { id: '3', sdk: 'Prebid', ad: 'Placeholder for Prebid Ad' },
  { id: '4', sdk: 'GAM', ad: 'Placeholder for GAM Auction' },
];

const AdScreen = () => {
  const [ads, setAds] = useState([]);
  const [isAdsInitialized, setIsAdsInitialized] = useState(false);

  useEffect(() => {
    initializeAds();
  }, []);

  const initializeAds = async () => {
    try {
      await MobileAds().initialize();
      setIsAdsInitialized(true);
    } catch (error) {
      console.error('Failed to initialize ads:', error);
    }
  };

  const loadAds = () => {
    if (ads.length > 0) {
      setAds([]);
    } else {
      setAds([
        { id: '1', sdk: 'GMA', ad: 'Banner Ad' },
        ...placeholderAds,
      ]);
    }
  };

  const renderItem = ({ item }) => {
    if (item.sdk === 'GMA' && isAdsInitialized) {
      return (
        <View style={styles.adContainer}>
          <Text style={styles.sdkName}>GMA</Text>
          <View style={styles.adWrapper}>
            <BannerAd
              unitId={GMA_BANNER_AD_UNIT_ID}
              size={BannerAdSize.BANNER}
              requestOptions={{
                requestNonPersonalizedAdsOnly: true,
              }}
            />
          </View>
        </View>
      );
    }

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
        <Text style={styles.buttonText}>
          {ads.length > 0 ? 'Clear Ads' : 'Load Ads'}
        </Text>
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
  adWrapper: {
    alignItems: 'center',
    justifyContent: 'center',
    marginTop: 8,
  },
  sdkName: {
    fontSize: 16,
    fontWeight: 'bold',
    marginBottom: 8,
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

export default AdScreen;
