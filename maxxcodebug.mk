# Copyright (c) 2026 Anshuman X (maxxcodebug). All rights reserved.
# vendor/maxxcodebug/maxxcodebug.mk
# Include from your device/vendor makefile:
#   $(call inherit-product, vendor/maxxcodebug/maxxcodebug.mk)

# Build type is picked up automatically from the environment.
#   export ANSHUMANX_BUILD_TYPE=OFFICIAL | UNOFFICIAL | PERSONAL
#   export ANSHUMANX_BUILD_FOR="Name"   (PERSONAL builds only)
ANSHUMANX_BUILD_TYPE ?= UNOFFICIAL
ANSHUMANX_BUILD_FOR  ?=

PRODUCT_SYSTEM_PROPERTIES += \
    ro.anshumanx.build.type=$(ANSHUMANX_BUILD_TYPE) \
    ro.anshumanx.build.for=$(ANSHUMANX_BUILD_FOR) \
    ro.anshumanx.build.date=$(shell date +%Y-%m-%d)

PRODUCT_PACKAGES += XHub
