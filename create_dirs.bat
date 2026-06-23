@echo off
REM Create event directories structure
cd /d c:\Users\Youssef\Documents\Projects\SmartStock\docs

echo Creating events directory...
if not exist events mkdir events

echo Creating subdirectories...
if not exist events\identity mkdir events\identity
if not exist events\product mkdir events\product
if not exist events\inventory mkdir events\inventory
if not exist events\warehouse mkdir events\warehouse
if not exist events\supplier mkdir events\supplier
if not exist events\customer mkdir events\customer
if not exist events\purchase-order mkdir events\purchase-order
if not exist events\sales-order mkdir events\sales-order

echo.
echo Verifying directories...
echo.
dir /b /ad events

echo.
echo Done!
