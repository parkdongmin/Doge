# R8 규칙. 사용하는 라이브러리(Room·Hilt·WorkManager·kotlinx.serialization·Firebase·AdMob·Coil)는
# 각자 소비자 규칙을 AAR에 포함하고 있어 따로 keep할 것이 없다. 앱 코드에 리플렉션은 없다.
# 문제가 생겨 규칙을 추가할 땐 어떤 증상 때문에 넣는지 주석으로 남길 것.

# Play Console 비정상 종료 보고에서 줄 번호가 보이도록(매핑 파일은 AAB에 자동 포함된다).
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
