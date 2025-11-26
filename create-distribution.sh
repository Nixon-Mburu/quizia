#!/bin/bash
# Create distribution packages

BUILD_DIR="/home/nixon-wainaina/NetBeansProjects/quizia/quizia_frontend/app/build/libs"
DIST_DIR="/tmp/quizia-dist"

mkdir -p "$DIST_DIR"

echo "Creating distribution packages..."


echo "📦 Creating Windows package..."
mkdir -p "$DIST_DIR/quizia-windows-1.0.0"
cp "$BUILD_DIR"/quizia-frontend-all.jar "$DIST_DIR/quizia-windows-1.0.0/"
cp "$BUILD_DIR"/quizia.bat "$DIST_DIR/quizia-windows-1.0.0/"
cp "$BUILD_DIR"/quizia_installer.nsi "$DIST_DIR/quizia-windows-1.0.0/"
cp "$BUILD_DIR"/install.bat "$DIST_DIR/quizia-windows-1.0.0/"
cp "$BUILD_DIR"/WINDOWS_INSTALL.md "$DIST_DIR/quizia-windows-1.0.0/"
cp "$BUILD_DIR"/README.md "$DIST_DIR/quizia-windows-1.0.0/"

cd "$DIST_DIR"
zip -r quizia-windows-1.0.0.zip quizia-windows-1.0.0/
echo "✅ Created: quizia-windows-1.0.0.zip"


echo "📦 Creating Linux package..."
mkdir -p "$DIST_DIR/quizia-linux-1.0.0"
cp "$BUILD_DIR"/quizia_1.0.0_all.deb "$DIST_DIR/quizia-linux-1.0.0/"
cp "$BUILD_DIR"/quizia.sh "$DIST_DIR/quizia-linux-1.0.0/"
cp "$BUILD_DIR"/quizia-frontend-all.jar "$DIST_DIR/quizia-linux-1.0.0/"
cp "$BUILD_DIR"/LINUX_INSTALL.md "$DIST_DIR/quizia-linux-1.0.0/"
cp "$BUILD_DIR"/README.md "$DIST_DIR/quizia-linux-1.0.0/"

zip -r quizia-linux-1.0.0.zip quizia-linux-1.0.0/
echo "✅ Created: quizia-linux-1.0.0.zip"


echo "📦 Creating Universal package..."
mkdir -p "$DIST_DIR/quizia-universal-1.0.0"
cp "$BUILD_DIR"/quizia-frontend-all.jar "$DIST_DIR/quizia-universal-1.0.0/"
cp "$BUILD_DIR"/quizia.sh "$DIST_DIR/quizia-universal-1.0.0/"
cp "$BUILD_DIR"/README.md "$DIST_DIR/quizia-universal-1.0.0/"
cp "$BUILD_DIR"/DISTRIBUTION_GUIDE.md "$DIST_DIR/quizia-universal-1.0.0/"

zip -r quizia-universal-1.0.0.zip quizia-universal-1.0.0/
echo "✅ Created: quizia-universal-1.0.0.zip"


echo ""
echo "=========================================="
echo "Distribution packages created!"
echo "=========================================="
ls -lh "$DIST_DIR"/*.zip
echo ""
echo "Location: $DIST_DIR"
echo ""
echo "Ready to distribute!"
