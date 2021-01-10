// wdio.dev.config.js
var merge = require('deepmerge');
var wdioConf = require('./wdio.conf.js');

// have main config file as default but overwrite environment specific information
exports.config = merge(wdioConf.config, {
  capabilities: [{
    app: '../../../../../build/osx/react/samples/ReactTemplateProject/youi/Release/kaltura-player-video-test-app',
    automationName: 'YouiEngine',
    deviceName: 'Mac',
    platformName: 'yimac',
    youiEngineAppAddress: 'localhost'
  }],

}, { clone: false });
