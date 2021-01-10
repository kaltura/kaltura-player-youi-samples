import React from 'react';
import { View, StyleSheet, Text, SectionList } from 'react-native';
import {MyTouchableOpacity} from './index.youi';

export const SOURCE_TYPE = {
    source: "source",
    media: "media"
};

const DATA = [
    {
        title: 'External source',
        playerConfig: {
            partnerId: 3009,
            initOptions : {
                "serverUrl": "https://rest-us.ott.kaltura.com/v4_5/api_v3/",
                "autoplay": true,
                "preload": true,
                "allowCrossProtocolRedirect": true,
                "allowFairPlayOnExternalScreens": true,
                "shouldPlayImmediately": true,
                "networkSettings": {
                    "autoBuffer": true,
                    "preferredForwardBufferDuration": 30000,
                    "automaticallyWaitsToMinimizeStalling": true
                },
                "abrSettings": {
                    "minVideoBitrate": 600000,
                    "maxVideoBitrate": 1500000
                },
                "trackSelection": {
                    "textMode": "AUTO",
                    "textLanguage": "en",
                    "audioMode": "AUTO",
                    "audioLanguage": "en",
                },
                "plugins": {
                    "ima": {},
                    "youbora": {
                        "accountCode": "kalturatest"
                    }
                }
            }
        },
        data: [{
            id: 'bd7acbea-c1b1-46c2-aed5-3ad53abb28ba',
            title: 'Source',
            type: SOURCE_TYPE.source,
            data: {
                sources: {
                    uri: "https://qa-apache-php7.dev.kaltura.com/p/1091/sp/109100/playManifest/entryId/0_wifqaipd/protocol/https/format/applehttp/flavorIds/0_h65mfj7f,0_3flmvnwc,0_m131krws,0_5407xm9j,0_xcrwyk2n/a.m3u8"
                }
            }
        }]
    },
    {
        title: 'Account 3009',
        playerConfig: {
            partnerId: 3009,
            initOptions : {
                "serverUrl": "https://rest-us.ott.kaltura.com/v4_5/api_v3/",
                "autoplay": true,
                "preload": true,
                "manageKeepScreenOnInternally": true,
                "allowCrossProtocolRedirect": true,
                "allowFairPlayOnExternalScreens": true,
                "shouldPlayImmediately": true,
                "networkSettings": {
                    "autoBuffer": true,
                    "preferredForwardBufferDuration": 30000,
                    "automaticallyWaitsToMinimizeStalling": true
                },
                "abrSettings": {
                    "minVideoBitrate": 600000,
                    "maxVideoBitrate": 1500000
                },
                "trackSelection": {
                    "textMode": "AUTO",
                    "textLanguage": "en",
                    "audioMode": "AUTO",
                    "audioLanguage": "en",
                },
                "plugins": {
                    "ima": {},
                    "youbora": {
                        "accountCode": "kalturatest",
                        "username": "aaa",
                        "userEmail": "aaa@gmail.com",
                        "userType": "paid",       // optional any string - free / paid etc.
                        "houseHoldId": "qwerty",   // optional which device is used to play
                        "httpSecure": true,        // youbora events will be sent via https
                        "appName": "YouiBridgeTesdtApp",
                        "appReleaseVersion": "v1.0.0",
                    }
                }
            }
        },
        data: [{
            id: '3ac68afc-c605-48d3-a4f8-fbd91aa97f63',
            title: 'Media',
            type: SOURCE_TYPE.media,
            data: {
                media: {
                    id: "548576",
                    asset: {
                        "format": "Mobile_Main",
                        "assetType": "media",
                        "protocol": "http",
                        "playbackContextType": "playback",
                        "urlType": "DIRECT",
                        "startPosition": 0,
                        "plugins": {
                            // "ima": {
                            //     "adTagUrl": "https://pubads.g.doubleclick.net/gampad/ads?sz=640x480&iu=/124319096/external/single_ad_samples&ciu_szs=300x250&impl=s&gdfp_req=1&env=vp&output=vast&unviewed_position_start=1&cust_params=deployment%3Ddevsite%26sample_ct%3Dskippablelinear&correlator=",
                            //     "alwaysStartWithPreroll": true,
                            //     "enableDebugMode": false
                            // },
                            "youbora": {
                                "extraParam1": "param1",
                                "extraParam2": "param2"
                            }
                        }
                    }
                }
            }
        }, {
            id: '3ac68afc-c605-48d3-a4f8-fbd91aa97f63',
            title: 'Media 2',
            type: SOURCE_TYPE.media,
            data: {
                media: {
                    id: "548579",
                    asset: {
                        "format": "Mobile_Main",
                        "assetType": "media",
                        "protocol": "http",
                        "playbackContextType": "playback",
                        "urlType": "DIRECT",
                        "startPosition": 0,
                        "plugins": {
                            "youbora": {
                                "extraParam1": "param3",
                                "extraParam2": "param4"
                            }
                        }
                    }
                }
            }
        }]
    }
];

function Item({ title, data, onPress }) {
    return (
        <View style={styles.item}>
            <MyTouchableOpacity text={title} style={styles.title} onPress={() => {onPress(data)}}/>
        </View>
    );
}

function VideoGallery(props) {
    return (
        <SectionList
            style={styles.list}
            sections={DATA}
            renderItem={({ item, section }) => <Item title={item.title} data={{...section.playerConfig, ...item.data}} onPress={props.onVideoSelected}/>}
            renderSectionHeader={({ section: { title } }) => (
                <Text style={styles.header}>{title}</Text>
            )}
            keyExtractor={(item, index) => item + index}
        />
    );
}

const styles = StyleSheet.create({
    list: {
        backgroundColor: 'gray'
    },
    header: {
        fontSize: 20,
        textAlign: 'center',
    },
    item: {
        backgroundColor: 'white',
        padding: 6,
        marginVertical: 4,
        marginHorizontal: 16,
    },
    title: {
        fontSize: 16,
    },
});

export default VideoGallery;
