# LocaFi - WiFi-Based Location Tracking

## Overview
LocaFi is an Android application that calculates user location purely through WiFi signal analysis. The app's core functionality relies on detecting nearby WiFi networks and using their signal strengths to determine the user's position. As an auxiliary feature, the app offers the ability to compare this WiFi-based location with the device's actual GPS location, allowing users to assess the accuracy of the WiFi positioning system.

## Screenshots

### Map View with GPS Hidden
![Map View with GPS Hidden](https://raw.githubusercontent.com/username/repository/branch/image1.png)

### Map View with GPS Shown
![Map View with GPS Shown](https://raw.githubusercontent.com/username/repository/branch/image2.png)

## Key Features

### WiFi Signal Analysis
- Real-time scanning and detection of nearby WiFi networks
- Signal strength visualization with color-coded indicators
- Detailed network information including SSID, BSSID, and signal level
- Signal strength classification (Strong, Good, Fair, Poor, Very Poor)

### Location Tracking
- Primary WiFi-based position calculation using signal strength analysis
- Advanced triangulation using multiple WiFi access points
- Position estimation through signal strength to distance conversion
- Optional GPS comparison feature:
  - Ability to display actual GPS location alongside WiFi-calculated position
  - Distance measurement between WiFi-calculated and GPS positions
  - Visual aids for accuracy assessment

### Interactive Map Interface
- Google Maps integration for visual location tracking
- Custom markers for WiFi access points and estimated positions
- Signal coverage visualization with radius indicators
- Location accuracy circles based on signal strength

### Real-time Monitoring
- Continuous WiFi network scanning (10-second intervals)
- Dynamic signal strength updates
- Live location tracking and comparison
- Automatic position recalculation based on signal changes

### User Interface
- Material Design components for modern Android look
- Recycler view list of detected WiFi networks
- Signal strength indicators with progress bars
- Easy toggle between GPS and WiFi-based locations

## Technical Implementation

### Location Calculation
- Signal strength to distance conversion using path loss model
- Weighted centroid algorithm for position estimation
- Signal strength classification based on RSSI values
- Accuracy radius calculation based on signal quality

### Permissions
- Location permissions (Fine and Coarse)
- WiFi state access permissions
- Dynamic permission handling with user-friendly dialogs

### Services
- Background service for continuous WiFi scanning
- Efficient battery usage with optimized scan intervals
- Proper service lifecycle management

## Requirements
- Android SDK 26 or higher
- Google Play Services (Maps)
- Location Services enabled
- WiFi capability

## Privacy Considerations
- Location data is processed locally on the device
- No external data storage or transmission
- User control over location services
- Clear permission request dialogs

## Installation
1. Enable location services on your device
2. Grant necessary permissions when prompted
3. Ensure WiFi is enabled for network detection
