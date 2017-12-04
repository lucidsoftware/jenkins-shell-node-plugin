-allowaccessmodification
-dontoptimize # otherwise Jenkins has class loading error
-keep public class com.lucidchart.jenkins.shellnode.** {
  public *;
}
#-keep public class * extends hudson.model.Descriptor {
#    *;
#}
-keepclassmembers class * {
    ** MODULE$;
}
#-keepclasseswithmembers public class * {
#    @org.kohsuke.stapler.DataBoundConstructor public *;
#}
#-keepclasseswithmembers public class * {
#  public ** do*(...);
#}
-keep,allowoptimization,allowshrinking class * { *; }
-keepattributes *
-optimizationpasses 1
-target 8
-verbose
