# EShop (FashionHub) ඇන්ඩ්‍රොයිඩ් ප්‍රොජෙක්ට් විශ්ලේෂණය

මෙය **EShop (FashionHub)** නම් වූ Android E-Commerce යෙදුමක (App) සම්පූර්ණ විශ්ලේෂණයකි. මෙම යෙදුම Java ක්‍රමලේඛන භාෂාව (Programming Language) භාවිතා කරමින් නිර්මාණය කර ඇත. මෙහි ප්‍රධාන අරමුණ වන්නේ පාරිභෝගිකයින්ට අන්තර්ජාලය හරහා ඇඳුම් හෝ වෙනත් භාණ්ඩ මිලදී ගැනීමට පහසුකම් සැලසීමයි.

## 1. මූලික තොරතුරු (Basic Information)
* **Application ID / Package Name:** `lk.prabhath.fashionhub`
* **භාවිතා කර ඇති භාෂාව (Language):** Java
* **අවම Android සංස්කරණය (Min SDK):** Android 7.0 (API Level 24)
* **ඉලක්කගත Android සංස්කරණය (Target SDK):** Android 15 (API Level 36)

## 2. භාවිතා කර ඇති ප්‍රධාන තාක්ෂණයන් සහ Libraries
මෙම ප්‍රොජෙක්ට් එක සඳහා පහත සඳහන් Third-party Libraries සහ සේවාවන් භාවිතා කර ඇත:

* **Firebase Services:**
  * **Firebase Auth:** පරිශීලකයන්ට ලොග් වීමට සහ නව ගිණුම් (Register) සෑදීමට.
  * **Firestore & Storage:** භාණ්ඩ වල දත්ත (Product Data) සහ පින්තූර (Images) cloud එකෙහි ගබඩා කිරීමට.
  * **Firebase Messaging / OneSignal:** පරිශීලකයන්ට Push Notifications යැවීම සඳහා.
  * **Firebase Analytics:** යෙදුම භාවිතා කරන ආකාරය පිළිබඳ දත්ත එක්රැස් කිරීමට.
* **PayHere SDK (`v3.0.18`):** ශ්‍රී ලංකාව තුළ Online Payments (Credit/Debit Cards) මගින් මුදල් ගෙවීම් ආරක්ෂිතව සිදු කිරීම සඳහා.
* **Google Maps API:** පාරිභෝගිකයාගේ ස්ථානය (Location/Address) ලබාගැනීමට.
* **Glide සහ Picasso:** අන්තර්ජාලයේ ඇති පින්තූර යෙදුම තුළ වේගයෙන් සහ පහසුවෙන් load කිරීමට.
* **JavaMail API:** යෙදුම හරහා ස්වයංක්‍රීයව Emails යැවීමේ පහසුකම සඳහා.
* **Gson:** JSON දත්ත හසුරුවා ගැනීම සඳහා.

## 3. ඇප් එකෙහි අඩංගු ප්‍රධාන තිරයන් (Main Activities & UI Flow)
මෙම යෙදුම තුළ පාරිභෝගිකයාට භාවිතා කිරීමට පහත සඳහන් ප්‍රධාන අතුරුමුහුණත් (Screens/Activities) අඩංගු වේ:

1. **SplashActivity:** යෙදුම විවෘත කරන විට මුලින්ම තිරයේ දිස්වන ආරම්භක පිටුව.
2. **LoginActivity & RegisterActivity:** දැනට සිටින පරිශීලකයන්ට ලොග් වීමට සහ අලුතින් යෙදුම භාවිතා කරන අයට නව ගිණුමක් ආරම්භ කිරීමට.
3. **MainActivity (සහ HomeFragment):** යෙදුමේ ප්‍රධාන පිටුව මෙය වන අතර, භාණ්ඩ ලැයිස්තුව (Product List) මෙහි පෙන්වයි.
4. **ProductDetailsActivity:** යම් භාණ්ඩයක් තේරූ විට, එහි සම්පූර්ණ විස්තර, මිල සහ පින්තූර පෙන්වන පිටුව.
5. **CartActivity:** මිලදී ගැනීමට තෝරාගත් භාණ්ඩ රඳවා තබාගන්නා "Shopping Cart" පිටුව.
6. **CheckoutActivity:** භාණ්ඩ මිලදී ගැනීමට පෙර පාරිභෝගිකයාගේ ලිපිනය ඇතුළත් කරන සහ **PayHere** හරහා මුදල් ගෙවීම් කරන අවසන් පිටුව.
7. **OrdersActivity:** පරිශීලකයා විසින් මීට පෙර මිලදී ගත් භාණ්ඩ වල ඉතිහාසය (Order History) බැලීමට ඇති පිටුව.
8. **ProfileActivity:** පරිශීලකයාගේ පෞද්ගලික තොරතුරු සහ profile පින්තූරය වෙනස් කිරීමට ඇති පිටුව.

## 4. ලබාගෙන ඇති ප්‍රධාන අවසරයන් (App Permissions)
යෙදුම නිසි පරිදි ක්‍රියාත්මක වීම සඳහා පහත සඳහන් අවසරයන් (Permissions) Android උපාංගයෙන් ලබාගෙන ඇත:
* **Internet:** අන්තර්ජාලයට සම්බන්ධ වීමට (දත්ත ලබාගැනීමට සහ ගෙවීම් කිරීමට).
* **Camera & Read Media/Storage:** Profile පින්තූර හෝ වෙනත් පින්තූර ලබාගැනීමට සහ උඩුගත කිරීමට.
* **Location (Fine & Coarse):** Google Maps හරහා පාරිභෝගිකයාගේ නිවැරදි ස්ථානය ලබාගැනීමට (Order එක deliver කිරීම සඳහා).
* **Vibrate:** Notifications එන විට දුරකථනය Vibrate වීමට.

## සාරාංශය (Summary)
මෙම **FashionHub** ව්‍යාපෘතිය සම්පූර්ණයෙන්ම ක්‍රියාත්මක වන (Fully Functional) E-Commerce යෙදුමක් වන අතර, Firebase හරහා දත්ත කළමනාකරණය කරමින්, PayHere හරහා අන්තර්ජාල ගෙවීම් පහසුකම්ද සපයන සාර්ථක Android නිර්මාණයකි.
