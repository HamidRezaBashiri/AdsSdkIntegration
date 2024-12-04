import React, { useState, useEffect, useRef } from 'react';
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
} from 'react-native-google-mobile-ads';
import InMobiBanner from './components/InMobiBanner';

// Use the test Ad Unit ID for development
const GMA_BANNER_AD_UNIT_ID = __DEV__ 
  ? TestIds.BANNER 
  : 'ca-app-pub-XXXXX/YYYY';

const placeholderAds = [
  { id: '2', sdk: 'InMobi', ad: 'InMobi Banner Ad' },
  { id: '3', sdk: 'Prebid', ad: 'Placeholder for Prebid Ad' },
  { id: '4', sdk: 'GAM', ad: 'Placeholder for GAM Auction' },
];

const AdScreen = () => {
  const [ads, setAds] = useState([]);
  const [refreshKey, setRefreshKey] = useState(0);
  const refreshTimerRef = useRef(null);

  useEffect(() => {
    return () => {
      if (refreshTimerRef.current) {
        clearTimeout(refreshTimerRef.current);
      }
    };
  }, []);

  useEffect(() => {
    if (ads.length > 0) {
      refreshTimerRef.current = setTimeout(() => {
        console.log('Refreshing ads - 60s mark');
        setRefreshKey(prev => prev + 1);
        
        refreshTimerRef.current = setTimeout(() => {
          console.log('Refreshing ads - 180s mark');
          setRefreshKey(prev => prev + 1);
        }, 120000);
      }, 60000);
    }

    return () => {
      if (refreshTimerRef.current) {
        clearTimeout(refreshTimerRef.current);
      }
    };
  }, [ads]);

  const loadAds = () => {
    setAds([
      { id: '1', sdk: 'GMA', ad: 'Banner Ad' },
      ...placeholderAds,
    ]);
    setRefreshKey(prev => prev + 1);
  };

  const renderItem = ({ item }) => {
    if (item.sdk === 'GMA') {
      return (
        <View style={styles.adContainer}>
          <Text style={styles.sdkName}>GMA</Text>
          <View style={styles.adWrapper}>
            <BannerAd
              key={`banner-${refreshKey}`}
              unitId={"ca-app-pub-2726091127303444/23201868728"}
              size={BannerAdSize.BANNER}
              requestOptions={{
                requestNonPersonalizedAdsOnly: true,
              }}
            />
          </View>
        </View>
      );
    }

    if (item.sdk === 'InMobi') {
      return (
        <View style={styles.adContainer}>
          <Text style={styles.sdkName}>InMobi</Text>
          <View style={styles.adWrapper}>
            <InMobiBanner
              key={`inmobi-${refreshKey}`}
              style={styles.banner}
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
  banner: {
    width: 320,
    height: 50,
    alignSelf: 'center',
  },
});

export default AdScreen;
