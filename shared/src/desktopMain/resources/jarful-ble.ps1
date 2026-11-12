# Jarful BLE helper for Windows (WinRT via Windows PowerShell 5.1). No native code, no extra installs.
#   list                              -> prints "<address>|<name>" for nearby and known BLE devices
#   write <addr> <charUuid|auto> <file> <chunk> -> writes the file's bytes to the characteristic in chunks
#   services <addr>                   -> prints services/characteristics (diagnostics)
param(
    [Parameter(Position = 0)] [string] $Mode,
    [Parameter(Position = 1)] [string] $Address = "",
    [Parameter(Position = 2)] [string] $CharUuid = "auto",
    [Parameter(Position = 3)] [string] $DataFile = "",
    [Parameter(Position = 4)] [int] $Chunk = 20
)
$ErrorActionPreference = "Stop"
try { [Console]::OutputEncoding = [System.Text.Encoding]::UTF8 } catch { }
Add-Type -AssemblyName System.Runtime.WindowsRuntime
[Windows.Devices.Bluetooth.BluetoothLEDevice,Windows.Devices.Bluetooth,ContentType=WindowsRuntime] | Out-Null
[Windows.Devices.Bluetooth.GenericAttributeProfile.GattDeviceService,Windows.Devices.Bluetooth,ContentType=WindowsRuntime] | Out-Null
[Windows.Devices.Enumeration.DeviceInformation,Windows.Devices.Enumeration,ContentType=WindowsRuntime] | Out-Null
[Windows.Storage.Streams.DataWriter,Windows.Storage.Streams,ContentType=WindowsRuntime] | Out-Null
[Windows.Storage.Streams.IBuffer,Windows.Storage.Streams,ContentType=WindowsRuntime] | Out-Null
[Windows.Devices.Bluetooth.GenericAttributeProfile.GattWriteResult,Windows.Devices.Bluetooth,ContentType=WindowsRuntime] | Out-Null

$asTaskGeneric = ([System.WindowsRuntimeSystemExtensions].GetMethods() | Where-Object {
    $_.Name -eq 'AsTask' -and $_.GetParameters().Count -eq 1 -and $_.GetParameters()[0].ParameterType.Name -eq 'IAsyncOperation`1'
})[0]
function Await($WinRtTask, $ResultType) {
    $asTask = $asTaskGeneric.MakeGenericMethod($ResultType)
    $netTask = $asTask.Invoke($null, @($WinRtTask))
    if (-not $netTask.Wait(20000)) { throw "TIMEOUT" }
    return $netTask.Result
}

$known = @(
    "0000ff02-0000-1000-8000-00805f9b34fb", "0000ae01-0000-1000-8000-00805f9b34fb", "00002af1-0000-1000-8000-00805f9b34fb",
    "49535343-8841-43f4-a8d4-ecbe34729bb3", "bef8d6c9-9c21-4c9e-b632-bd58c1009f9f", "6e400002-b5a3-f393-e0a9-e50e24dcca9e"
)

function Initialize-BleDiscovery {
    # PowerShell 5.1 cannot subscribe to WinRT events, and scriptblock callbacks on a
    # Bluetooth worker thread have no runspace. Keep callbacks in managed C# instead.
    if (-not ('Jarful.BleDiscovery' -as [type])) {
        $source = @'
using System;
using System.Collections.Generic;
using System.Threading;
using Windows.Devices.Bluetooth;
using Windows.Devices.Bluetooth.Advertisement;
using Windows.Foundation;

namespace Jarful {
    public sealed class BleAdvertisement {
        public ulong Address;
        public string Name;
        public BluetoothAddressType AddressType;
    }
    public static class BleDiscovery {
        public static BleAdvertisement[] Scan(ulong targetAddress, int durationMs) {
            var devices = new Dictionary<ulong, BleAdvertisement>();
            var gate = new object();
            var finished = new ManualResetEvent(false);
            bool closed = false;
            BluetoothError error = BluetoothError.Success;
            var watcher = new BluetoothLEAdvertisementWatcher();
            watcher.ScanningMode = BluetoothLEScanningMode.Active;
            TypedEventHandler<BluetoothLEAdvertisementWatcher, BluetoothLEAdvertisementReceivedEventArgs> received = (sender, args) => {
                lock (gate) {
                    if (closed) return;
                    BleAdvertisement device;
                    if (!devices.TryGetValue(args.BluetoothAddress, out device)) {
                        device = new BleAdvertisement { Address = args.BluetoothAddress, Name = "" };
                        devices.Add(device.Address, device);
                    }
                    if (!String.IsNullOrWhiteSpace(args.Advertisement.LocalName)) device.Name = args.Advertisement.LocalName;
                    device.AddressType = args.BluetoothAddressType;
                    if (targetAddress != 0 && device.Address == targetAddress) finished.Set();
                }
            };
            TypedEventHandler<BluetoothLEAdvertisementWatcher, BluetoothLEAdvertisementWatcherStoppedEventArgs> stopped = (sender, args) => {
                lock (gate) {
                    if (closed) return;
                    error = args.Error;
                    finished.Set();
                }
            };
            watcher.Received += received;
            watcher.Stopped += stopped;
            try {
                watcher.Start();
                finished.WaitOne(durationMs);
                lock (gate) {
                    if (error != BluetoothError.Success) throw new InvalidOperationException("BLE_SCAN_" + error);
                    var result = new BleAdvertisement[devices.Count];
                    devices.Values.CopyTo(result, 0);
                    return result;
                }
            } finally {
                lock (gate) { closed = true; }
                try { watcher.Stop(); }
                finally {
                    watcher.Received -= received;
                    watcher.Stopped -= stopped;
                    finished.Dispose();
                }
            }
        }
    }
}
'@
        # Use the .NET Framework compiler directly: Add-Type attempts to load .winmd
        # references as ordinary assemblies. All references ship with Windows 11.
        $compiler = New-Object Microsoft.CSharp.CSharpCodeProvider
        try {
            $parameters = New-Object System.CodeDom.Compiler.CompilerParameters
            $parameters.GenerateInMemory = $true
            foreach ($reference in @(
                "$env:SystemRoot\System32\WinMetadata\Windows.Devices.winmd",
                "$env:SystemRoot\System32\WinMetadata\Windows.Foundation.winmd",
                [System.Runtime.InteropServices.WindowsRuntime.WindowsRuntimeMarshal].Assembly.Location,
                [Reflection.Assembly]::Load('System.Runtime, Version=4.0.0.0, Culture=neutral, PublicKeyToken=b03f5f7f11d50a3a').Location,
                [Reflection.Assembly]::Load('System.Runtime.InteropServices.WindowsRuntime, Version=4.0.0.0, Culture=neutral, PublicKeyToken=b03f5f7f11d50a3a').Location
            )) { $parameters.ReferencedAssemblies.Add($reference) | Out-Null }
            $result = $compiler.CompileAssemblyFromSource($parameters, $source)
            if ($result.Errors.HasErrors) { throw "BLE_SCAN_HELPER_COMPILE: $($result.Errors -join '; ')" }
        } finally { $compiler.Dispose() }
    }
}

function Find-BleAdvertisements([UInt64] $TargetAddress = 0) {
    Initialize-BleDiscovery
    return [Jarful.BleDiscovery]::Scan($TargetAddress, 6000)
}

function Get-AddressNumber([string] $addr) {
    $hex = $addr -replace '[:\-]', ''
    if ($hex -notmatch '^[0-9a-fA-F]{12}$') { throw "INVALID_BLUETOOTH_ADDRESS: $addr" }
    return [Convert]::ToUInt64($hex, 16)
}

function Format-Address([UInt64] $num) {
    return ($num.ToString('X12') -replace '(..)(?!$)', '$1:')
}

function Open-BleDevice([UInt64] $num, $addressType = $null) {
    $operation = if ($null -eq $addressType) {
        [Windows.Devices.Bluetooth.BluetoothLEDevice]::FromBluetoothAddressAsync($num)
    } else {
        [Windows.Devices.Bluetooth.BluetoothLEDevice]::FromBluetoothAddressAsync($num, $addressType)
    }
    return Await $operation ([Windows.Devices.Bluetooth.BluetoothLEDevice])
}

function Get-Device([string] $addr) {
    $num = Get-AddressNumber $addr
    $dev = Open-BleDevice $num
    if ($null -eq $dev) {
        # FromBluetoothAddressAsync returns null for unpaired devices absent from
        # the system cache. A live scan populates that cache; pairing is not required.
        $nearby = @(Find-BleAdvertisements $num)
        $advertisement = $nearby | Where-Object { $_.Address -eq $num } | Select-Object -First 1
        if ($null -ne $advertisement) { $dev = Open-BleDevice $num $advertisement.AddressType }
        else { $dev = Open-BleDevice $num }
    }
    if ($null -eq $dev) {
        throw "DEVICE_NOT_FOUND: $(Format-Address $num) (turn the printer on, disconnect phone apps, then refresh the Bluetooth LE list and select its LE address; a Classic/COM address may differ)"
    }
    return $dev
}

function Get-KnownBleDevices {
    foreach ($paired in @($true, $false)) {
        $sel = [Windows.Devices.Bluetooth.BluetoothLEDevice]::GetDeviceSelectorFromPairingState($paired)
        $infos = Await ([Windows.Devices.Enumeration.DeviceInformation]::FindAllAsync($sel)) ([Windows.Devices.Enumeration.DeviceInformationCollection])
        foreach ($i in $infos) {
            if ($i.Id -match '-((?:[0-9a-f]{2}:){5}[0-9a-f]{2})$') {
                [PSCustomObject]@{ Address = (Get-AddressNumber $Matches[1]); Name = $i.Name }
            }
        }
    }
}

function Get-BleDeviceList {
    $devices = @{}
    $nearby = @(Find-BleAdvertisements)
    # Some Bluetooth drivers fail enumeration for an unsupported pairing selector.
    # That must not hide printers which were found in the live scan.
    $remembered = @(try { Get-KnownBleDevices } catch { Write-Verbose $_.Exception.Message })
    foreach ($device in $remembered + $nearby) {
        $mac = Format-Address $device.Address
        $name = $device.Name -replace '[\r\n|]', ' '
        if (-not [string]::IsNullOrWhiteSpace($name)) { $devices[$mac] = $name }
        elseif (-not $devices.ContainsKey($mac)) { $devices[$mac] = $mac }
    }
    foreach ($mac in @($devices.Keys | Sort-Object)) { Write-Output "$mac|$($devices[$mac])" }
}

function Get-Characteristics($dev) {
    $svcRes = Await ($dev.GetGattServicesAsync([Windows.Devices.Bluetooth.BluetoothCacheMode]::Uncached)) ([Windows.Devices.Bluetooth.GenericAttributeProfile.GattDeviceServicesResult])
    if ($svcRes.Status -ne 'Success') { throw "GATT_SERVICES_$($svcRes.Status)" }
    $all = @()
    foreach ($svc in $svcRes.Services) {
        $chRes = Await ($svc.GetCharacteristicsAsync([Windows.Devices.Bluetooth.BluetoothCacheMode]::Uncached)) ([Windows.Devices.Bluetooth.GenericAttributeProfile.GattCharacteristicsResult])
        if ($chRes.Status -eq 'Success') { foreach ($c in $chRes.Characteristics) { $all += $c } }
    }
    return $all
}

function Select-WriteCharacteristic($chars, [string] $uuid = 'auto') {
    $writable = @($chars | Where-Object { ($_.CharacteristicProperties -band 12) -ne 0 })
    $target = $null
    if ($uuid -ne 'auto') {
        $target = $writable | Where-Object { $_.Uuid.ToString() -eq $uuid } | Select-Object -First 1
    } else {
        foreach ($k in $known) {
            if ($null -eq $target) { $target = $writable | Where-Object { $_.Uuid.ToString() -eq $k } | Select-Object -First 1 }
        }
        if ($null -eq $target) { $target = $writable | Select-Object -First 1 }
    }
    if ($null -eq $target) { throw "NO_WRITABLE_CHARACTERISTIC" }
    return $target
}

$script:writeMode = ""

# PowerShell 5.1 cannot always bind the 2-argument WriteValueAsync overload; try several ways.
function Write-Buffer($target, $buffer, $option) {
    if ($script:writeMode -eq "" -or $script:writeMode -eq "reflect2") {
        $m2 = $target.GetType().GetMethod("WriteValueAsync", [Type[]]@([Windows.Storage.Streams.IBuffer], [Windows.Devices.Bluetooth.GenericAttributeProfile.GattWriteOption]))
        if ($null -ne $m2) {
            $status = Await ($m2.Invoke($target, @($buffer, $option))) ([Windows.Devices.Bluetooth.GenericAttributeProfile.GattCommunicationStatus])
            $script:writeMode = "reflect2"; return $status
        }
    }
    if ($script:writeMode -eq "" -or $script:writeMode -eq "withresult") {
        try {
            $res = Await ($target.WriteValueWithResultAsync($buffer, $option)) ([Windows.Devices.Bluetooth.GenericAttributeProfile.GattWriteResult])
            $script:writeMode = "withresult"; return $res.Status
        } catch { if ($script:writeMode -eq "withresult") { throw } }
    }
    $status = Await ($target.WriteValueAsync($buffer)) ([Windows.Devices.Bluetooth.GenericAttributeProfile.GattCommunicationStatus])
    $script:writeMode = "one-arg"
    return $status
}

function Write-Chunks($target, [byte[]] $bytes, [int] $chunkSize) {
    if ($chunkSize -lt 1) { throw "INVALID_CHUNK_SIZE" }
    $noResp = ($target.CharacteristicProperties -band [Windows.Devices.Bluetooth.GenericAttributeProfile.GattCharacteristicProperties]::WriteWithoutResponse) -ne 0
    $option = if ($noResp) { [Windows.Devices.Bluetooth.GenericAttributeProfile.GattWriteOption]::WriteWithoutResponse } else { [Windows.Devices.Bluetooth.GenericAttributeProfile.GattWriteOption]::WriteWithResponse }
    $i = 0
    while ($i -lt $bytes.Length) {
        $len = [Math]::Min($chunkSize, $bytes.Length - $i)
        $part = New-Object byte[] $len
        [Array]::Copy($bytes, $i, $part, 0, $len)
        $writer = New-Object Windows.Storage.Streams.DataWriter
        $writer.WriteBytes($part)
        $status = Write-Buffer $target ($writer.DetachBuffer()) $option
        if ("$status" -ne 'Success') { throw "WRITE_FAILED_$status at byte $i (mode $script:writeMode)" }
        $i += $len
        if ($noResp) { Start-Sleep -Milliseconds 12 }
    }
}

try {
switch ($Mode) {
    "list" {
        Get-BleDeviceList
    }
    "services" {
        $dev = Get-Device $Address
        Write-Output "device: $($dev.Name) [$Address] connection=$($dev.ConnectionStatus)"
        $chars = Get-Characteristics $dev
        foreach ($c in $chars) {
            $mark = if ($known -contains $c.Uuid.ToString().ToLower()) { " <- known printer characteristic" } else { "" }
            Write-Output ("  char {0} [{1}]{2}" -f $c.Uuid, $c.CharacteristicProperties, $mark)
        }
        # A GB01 probe is not valid for M02 or other printer protocols. Diagnostics
        # report the selected channel without sending protocol-specific bytes.
        $target = Select-WriteCharacteristic $chars
        Write-Output ("write characteristic: {0}" -f $target.Uuid)
    }
    "write" {
        $dev = Get-Device $Address
        $chars = Get-Characteristics $dev
        $target = Select-WriteCharacteristic $chars $CharUuid
        $bytes = [System.IO.File]::ReadAllBytes($DataFile)
        Write-Chunks $target $bytes $Chunk
        Start-Sleep -Milliseconds 400
        Write-Output "OK $($bytes.Length) (mode $script:writeMode)"
    }
    default { throw "USAGE: list | services <addr> | write <addr> <char|auto> <file> <chunk>" }
}
} catch {
    $msg = $_.Exception.Message
    $pos = if ($_.InvocationInfo) { $_.InvocationInfo.PositionMessage -replace "`r?`n", " " } else { "" }
    Write-Output ("ERROR: {0} {1}" -f $msg, $pos)
    exit 1
}
