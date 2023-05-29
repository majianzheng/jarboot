<template>
  <pre class="terminal-banner">
<div class="red">


      .--.
    .--,`|
    |  |.
    '--`_
    ,--,'|
    |  | '
    :  | |
  __|  : '
.'__/\_: |
|   :    :
 \   \  /
  `--`-'
</div>
<div class="green">




   ,--.--.
   /       \
  .--.  .-. |
   \__\/: . .
   ," .--.; |
  /  /  ,.  |
 ;  :   .'   \
 |  ,     .-./
  `--`---'
</div>
<div class="yellow">


  __  ,-.
,' ,'/ /|
'  | |' |
|  |   ,'
'  :  /
|  | '
;  : |
|  , ;
 ---'
</div>
<div class="blue">

  ,---,
,---.'|
|   | :
:   : :
:     |,-.
|   : '  |
|   |  / :
'   : |: |
|   | '/ :
|   :    |
/    \  /
`-'----'
</div>
<div class="magenta">


   ,---.
  '   ,'\
 /   /   |
.   ; ,. :
'   | |: :
'   | .; :
|   :    |
 \   \  /
  `----'
</div>
<div class="cyan">


   ,---.
  '   ,'\
 /   /   |
.   ; ,. :
'   | |: :
'   | .; :
|   :    |
 \   \  /
  `----'
</div>
<div class="red">
    ___
  ,--.'|_
  |  | :,'
  :  : ' :
.;__,'  /
|  |   |
:__,'| :
  '  : |__
  |  | '.'|
  ;  :    ;
  |  ,   /
   ---`-'
</div>
  <div>



 version: v{{versionStore.version}}

 Jarboot console, docs: <el-button @click="openDocs" link class="cyan">{{ DOCS_URL }}</el-button>

 Diagnose command, type ‘help’ and hit ‘ENTER’ to see.
  </div>
</pre>
</template>

<script setup lang="ts">
import { DOCS_URL } from '@/common/CommonConst';
import { useBasicStore } from '@/stores';
const versionStore = useBasicStore();
const openDocs = () => window.open(DOCS_URL);
</script>

<style lang="less" scoped>
.terminal-banner {
  display: flex;
  white-space: pre;
  tab-size: 4;
  word-wrap: normal;
  font-size: 13px;
  line-height: 1.42857143;
  word-break: break-all;
  margin: 0 6px;
  .black {
    color: black;
  }
  .red {
    color: red;
  }
  .green {
    color: lawngreen;
  }
  .yellow {
    color: yellow;
  }
  .blue {
    color: blueviolet;
  }
  .magenta {
    color: magenta;
  }
  .cyan {
    color: cyan;
  }
  .white {
    color: white;
  }
}
</style>
