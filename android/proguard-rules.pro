-dontobfuscate

#these are essential packages that should not be "optimized" in any way
#the main purpose of d8 here is to shrink the absurdly-large google play games libraries
-keep class mindustry.** { *; }
-keep class mindustryX.** { *; }
-keep class arc.** { *; }
-keep class net.jpountz.** { *; }
-keep class rhino.** { *; }
-keep class com.android.dex.** { *; }
-keep class kotlin.** { *; }
-keep class org.jetbrains.annotations.** { *; }
-keep class org.intellij.lang.annotations.** { *; }

-dontwarn javax.naming.**
#-printusage out.txt