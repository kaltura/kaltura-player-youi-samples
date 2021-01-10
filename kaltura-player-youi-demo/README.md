# Packaging Sample Project

## Overview

This sample project implements a hierarchical set of packages, consumed by a top level You.i app.

- the project is built using `You.i Engine One v6.2.0`
- this sample only implements an iOS app, platform specific work is still required for further platforms
- there are 3 parts in the hierarchy:
   - a top level You.i app
   - an intermediate level You.i video component (You.i custom video player) package
   - a low level Kaltura Player frameworks package

- The You.i app consumes the You.i video component package
- The You.i video component package consumes Kaltura Player frameworks package
- The Kaltura Player frameworks package contains scripting to build frameworks for consumption

At a high level conceptual view:

```You.i app (eg O2)  <===  You.i video component  <===  Kaltura Player frameworks```


In this sample project, package dependencies are managed, via `npm` or `yarn`, using:
```
	package.json
```
And the build process (`cmake`) is managed using:
```
	ModuleList.cmake
	platform specific cmake (eg. YiIos.cmake)
```

## Package: Kaltura Player Frameworks

- the scripting to build all the required frameworks must be available in a registry
- 'registry' can be a folder on disk, to demonstrate how this packaging works (a remote repo can also be used)

- The `scripts` folder, originally from the `kaltura-player-youi` project, was copied into this project
- The `ios-libs/build.sh` script was modified to accept a folder specification where the frameworks will be built
- The `ModuleLists.txt` was modified to set the frameworks path, for later use in the cmake process


- frameworks folder structure:
    - `ios-libs` - folder that will be created when building the Kaltura Player frameworks
	- `ModuleList.cmake` - cmake module to:
		- set the frameworks path for the cmake process that will be consuming the frameworks
	- `package.json`: 
		- minimal implementation
		- identifies this project as an npm package
		- contains metadata about the package
		- run the frameworks build script before the cmake process, ensuring the frameworks are available at link time:
```
		"scripts": {
    		"postinstall": "./scripts/ios-libs/build.sh $PWD"
  		}
```

## Package: You.i video component

This package contains the You.i custom video player (KalturaVideo) that wraps the Kaltura Player frameworks
in a You.i component.

- `package.json`:
	- minimal implementation
	- identifies this project as an npm package
	- must contain the dependency for the frameworks:
```
		"dependencies": {
			"@kaltura-player/frameworks": "git+ssh://git@github.com:gordonc-youi/kaltura-frameworks.git#v0.3"	
		},
```
or :
```
		"dependencies": {
			"@kaltura-player/frameworks": "file:../kaltura-frameworks"	
		}
```
- I have tested with both approaches, and they work in the same manner


## You.i client app - using You.i video component

This project is a sample, demonstrating the packaging dependency and building an app that
can be run on an iOS device.

- package.json:
	- typical app package configuration
	- must contain the dependency for the You.i video component
	- the dependency on the You.i video component can be a local filesystem folder or a remote repo:
```
	"dependencies": {
		"@kaltura-player/video-component": "file:../kaltura-player-video-component",
		...
	},
```
- when initializing this project, the first step is to run `yarn install`
- failing to run `yarn install` will result in a link error during the build

- building this project for the first time, without an explicit `yarn install`, against:
	- You.i Engine 5.18 is successful
	- You.i Engine 6.2 will fail with a link error
		- the postinstall step built the frameworks properly
		- the frameworks never get moved to the correct target folder
		- the cmake build process causes the postinstall step to silently fail

- when running `yarn install`:
	- if the You.i video component dependency is not satisfied:
		- the You.i video component package will be downloaded into the `node_modules/@kaltura-player/video-component` folder
		- the Kaltura Player frameworks package will be downloaded into the `node_modules/@kaltura-player/frameworks` folder
		- the `postinstall` step in the frameworks package will run the `scripts/ios-libs/build.sh` script
		- the build script requires `cocoapods` to be installed on the local environment
		- the build script will take about 6 - 7 minutes

- after running `yarn install`, then run the build command:
```
	youi-tv build -p ios -c Release --clean --local
```


