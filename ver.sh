latest() {
  repo="$1"; ga="$2"
  path=$(echo "$ga" | sed 's/:/\//' | sed 's/\./\//g; s/\//./g' )
  grp=$(echo "$ga" | cut -d: -f1 | tr '.' '/')
  art=$(echo "$ga" | cut -d: -f2)
  url="$repo/$grp/$art/maven-metadata.xml"
  v=$(curl -s --max-time 20 "$url" | grep -oE '<version>[^<]+</version>' | sed 's/<[^>]*>//g' \
      | grep -viE 'alpha|beta|rc|dev|snapshot|eap|-M[0-9]' | tail -1)
  printf '%-62s %s\n' "$ga" "${v:-NOT FOUND}"
}
G="https://dl.google.com/dl/android/maven2"
C="https://repo1.maven.org/maven2"
