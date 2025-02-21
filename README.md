Here’s the refined **README** incorporating details about the **foreground service** and **Firebase real-time storage in the scanning page**:

---

# 📍 LocaFi – WiFi-Based Location Estimation

LocaFi is an **Android application** that enables **WiFi-based localization** by scanning and mapping nearby WiFi networks. Unlike traditional GPS-based tracking, this app estimates locations based on **WiFi signal strength**, making it useful in **indoor environments**, areas with poor GPS reception, and location-based research.

With **real-time scanning**, **Google Maps integration**, and **Firebase-powered data storage**, LocaFi provides an efficient way to visualize and analyze WiFi networks in different locations.


<p align="center">
  <img src="https://github.com/user-attachments/assets/f28ba081-8f0d-4a2e-b8f1-5718db5555f5" width="150">
</p>


---

## 🚀 Key Features

✔ **WiFi Scanning** – Detects nearby WiFi networks, capturing **SSID, BSSID, signal strength, and location**.  
✔ **Location Estimation** – Uses scanned WiFi networks to approximate the user's location.  
✔ **Google Maps Integration** – Displays detected networks on an **interactive map**.  
✔ **Background Scanning Service** – Utilizes a **foreground service** to continuously scan for WiFi networks, even when the app is minimized.  
✔ **Signal Strength Analysis** – Categorizes WiFi networks as **Excellent, Good, Fair, or Poor** based on signal strength.  
✔ **Firebase Realtime Database** – Saves scanned WiFi networks **immediately in Firebase**, allowing historical analysis and retrieval.  
✔ **Permission Management** – Ensures all required permissions (Location, WiFi, Background Services) are granted efficiently.  


---


## 📱 App Overview

### 🔍 **Scanning Page**
The **Scanning Page** enables users to **detect, list, and save available WiFi networks**. Key functionalities include:

- **Start Scan** – Initiates continuous **WiFi scanning**, running as a **foreground service** to detect networks in real-time.  
- **Stop Scan** – Stops the ongoing WiFi scan at any time.  
- **Clear Data** – Deletes previously scanned WiFi networks.  
- **Google Maps Integration** – Displays detected WiFi networks as markers on a map, with circle sizes based on signal strength.  
- **Firebase Realtime Storage** – Each detected WiFi network is **automatically saved** in **Firebase Realtime Database**, ensuring data persistence.  

The scanning process continues **even when the app is minimized** using a **foreground service**, allowing real-time updates while conserving battery.


<p align="center">
 <img src="https://github.com/user-attachments/assets/7efbc58b-1118-4008-84f4-c876abb29df2" width="150">
</p>



### 🏠 **Main Page**
The **Main Page** provides users with an overview of stored WiFi scans and estimated location data. Features include:

- **WiFi Network List** – Displays previously scanned WiFi networks, sorted by signal strength.  
- **Estimated Location** – Uses stored WiFi scans to calculate and display the user’s approximate location.  
- **Google Maps View** – Highlights scanned networks and provides **GPS vs. Estimated Location comparison** for accuracy evaluation.  
- **Error Distance Calculation** – Displays the distance between estimated and actual GPS locations, helping refine localization accuracy.  

The **Main Page** is designed for users who want to **analyze historical WiFi scans** and compare different locations.



<p align="center">
   <img src="https://github.com/user-attachments/assets/852732cc-d967-422f-9545-04c5fbda2de1" width="150">
   <img src="https://github.com/user-attachments/assets/6d8999f9-13d5-4e08-b869-5aa11f45bcf6" width="150">
</p>


---

## 🛠️ Technologies Used

- **Programming Language**: Java  
- **Framework**: Android SDK  
- **Database**: Firebase Realtime Database  
- **UI Components**: Material Design, RecyclerView, Google Maps API  
- **Background Services**: **Foreground service** for continuous WiFi scanning  

---

## ⚡ Foreground Service for Continuous Scanning

LocaFi runs a **foreground service** to perform **continuous WiFi scanning** in the background. This ensures that:

- **WiFi scanning continues even when the app is minimized.**  
- **Real-time updates** are available without requiring the app to stay open.  
- A **persistent notification** informs users that scanning is running.  

This service allows users to **seamlessly collect WiFi data** while using other apps.

---

## 🔧 Installation & Setup

1️⃣ **Clone the Repository**  
```bash
git clone https://github.com/YOUR_GITHUB_USERNAME/LocaFi.git
cd LocaFi
```

2️⃣ **Open the project in Android Studio** and ensure all dependencies are installed.  

3️⃣ **Set Up Firebase**  
- Add `google-services.json` to the `app/` directory.  
- Enable Firebase Realtime Database.  

4️⃣ **Build & Run** the app on an Android device or emulator with **WiFi and Location Services enabled**.

---

## 📌 Permissions Required

LocaFi requires the following **Android permissions** to function properly:

- **ACCESS_FINE_LOCATION** – Required for scanning WiFi networks and determining location.  
- **ACCESS_WIFI_STATE** – Allows the app to retrieve WiFi network details.  
- **FOREGROUND_SERVICE** – Enables background scanning while the app is running.  

Make sure to **grant all permissions** when prompted to enjoy the full functionality.

---

## 📜 License

This project is licensed under the **MIT License** – see the [LICENSE](LICENSE) file for details.

---

## 📧 Contact

For any inquiries, suggestions, or issues, feel free to open an **issue** on GitHub or reach out. 😊  

---

This version adds **foreground service details** and **Firebase integration explanation** while maintaining a **professional and approachable** tone. Let me know if you'd like any further refinements! 🚀
