#!/bin/sh
# create-release-key.sh creates the release key and the one-time rotation proof that
# lets installs signed with the old debug key accept updates signed with it.
#
# Run it once, through make release-key. Back up signing/release.jks and its password:
# losing either means every user has to reinstall.
set -eu

mkdir -p signing

keystore=signing/release.jks
[ ! -e "$keystore" ] || { echo "$keystore already exists" >&2; exit 1; }

apksigner=$(ls -d "$ANDROID_HOME"/build-tools/*/apksigner 2>/dev/null | sort -V | tail -n 1)
[ -x "$apksigner" ] || { echo "apksigner not found under \$ANDROID_HOME/build-tools" >&2; exit 1; }

# A key without its lineage is useless, so a failed run leaves neither behind.
trap 'stty echo 2>/dev/null; [ -f signing/lineage ] || rm -f "$keystore"' EXIT
trap 'exit 1' INT TERM

stty -echo
printf 'New release keystore password (at least 6 characters): '
read -r SASAYAKI_KEYSTORE_PASSWORD
printf '\nRepeat the password: '
read -r confirmation
printf '\n'
stty echo
[ "$SASAYAKI_KEYSTORE_PASSWORD" = "$confirmation" ] || { echo "The passwords differ" >&2; exit 1; }
export SASAYAKI_KEYSTORE_PASSWORD

keytool -genkeypair \
    -keystore "$keystore" \
    -storetype PKCS12 \
    -storepass:env SASAYAKI_KEYSTORE_PASSWORD \
    -alias sasayaki \
    -keyalg RSA \
    -keysize 4096 \
    -validity 36500 \
    -dname "CN=Sasayaki"

# The debug key is public, so it keeps only what the update itself needs: trust for the
# app's existing data. Without rollback, an APK signed by it alone is refused once a
# phone has installed a release signed with the new key.
"$apksigner" rotate \
    --out signing/lineage \
    --old-signer \
    --ks debug.keystore \
    --ks-key-alias androiddebugkey \
    --ks-pass pass:android \
    --key-pass pass:android \
    --set-installed-data true \
    --set-shared-uid false \
    --set-permission false \
    --set-rollback false \
    --set-auth false \
    --new-signer \
    --ks "$keystore" \
    --ks-key-alias sasayaki \
    --ks-pass env:SASAYAKI_KEYSTORE_PASSWORD \
    --key-pass env:SASAYAKI_KEYSTORE_PASSWORD

echo "Created $keystore and signing/lineage. Commit signing/lineage; never commit the keystore."
