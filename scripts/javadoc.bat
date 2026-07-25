@echo off
cd /d "%~dp0.."
echo Generating Javadoc...
call mvn javadoc:javadoc
echo Javadoc generation complete.
pause
