/**
 * 答辩语音自动生成脚本
 * 
 * 使用方法（Windows PowerShell）：
 *   $env:MINIMAX_API_KEY="your_api_key"; $env:MINIMAX_APP_ID="your_app_id"
 *   node generate_voice.js
 * 
 * 或创建 .env 文件（不要提交到Git！）：
 *   MINIMAX_API_KEY=your_api_key
 *   MINIMAX_APP_ID=your_app_id
 */

const https = require('https');
const fs = require('fs');
const path = require('path');
const { exit } = require('process');

// ==================== 配置 ====================
const API_KEY = process.env.MINIMAX_API_KEY || '';
const APP_ID = process.env.MINIMAX_APP_ID || '';

const VOICE_ID = 'male-qn-qingse';  // 默认男声-青年-清澈
const MODEL = 'speech-02-hd';        // 高质量模型

const AUDIO_DIR = path.join(__dirname);
const OUTPUT_FILES = [
  { textFile: '01_背景.txt', outFile: '01_背景.mp3' },
  { textFile: '02_架构.txt', outFile: '02_架构.mp3' },
  { textFile: '03_功能.txt', outFile: '03_功能.mp3' },
  { textFile: '04_数据库.txt', outFile: '04_数据库.mp3' },
  { textFile: '05_测试.txt', outFile: '05_测试.mp3' },
  { textFile: '06_总结.txt', outFile: '06_总结.mp3' },
];

// ==================== 工具函数 ====================

function log(msg) {
  console.log(`[${new Date().toLocaleTimeString('zh-CN')}] ${msg}`);
}

function logSuccess(file) {
  console.log(`  ✅ 已生成: ${file}`);
}

function logError(err) {
  console.error(`  ❌ 错误: ${err.message}`);
}

// 读取文本文件（自动去除BOM和多余空白）
function readTextFile(filepath) {
  let content = fs.readFileSync(filepath, 'utf8');
  content = content.replace(/^\uFEFF/, ''); // 去除UTF-8 BOM
  content = content.replace(/\r\n/g, '\n').replace(/\r/g, '\n');
  content = content.replace(/\n{3,}/g, '\n\n'); // 去除连续超过2个换行
  return content.trim();
}

// 调用 Minimax TTS API (T2A v2)
function callMinimaxTTS(text, voiceId = VOICE_ID) {
  return new Promise((resolve, reject) => {
    if (!API_KEY || !APP_ID) {
      reject(new Error('请先设置环境变量 MINIMAX_API_KEY 和 MINIMAX_APP_ID'));
      return;
    }

    const body = JSON.stringify({
      model: MODEL,
      text: text,
      stream: false,
      voice_setting: {
        voice_id: voiceId,
        speed: 0.85,       // 语速：0.5-2.0，设为0.85（偏慢，适合答辩）
        vol: 1.0,
        pitch: 0,
        // emotion: "happy"  // 可选情感参数
      },
      audio_setting: {
        sample_rate: 32000,
        bitrate: 128000,
        format: 'mp3',
        channel: 1  // 单声道
      }
    });

    const host = 'api.minimax.chat';
    const pathname = `/v1/t2a_v2?GroupId=${APP_ID}`;
    
    const options = {
      hostname: host,
      path: pathname,
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${API_KEY}`,
        'Content-Length': Buffer.byteLength(body)
      }
    };

    const req = https.request(options, (res) => {
      // Minimax T2A v2 返回的是 JSON，包含 audio_file 字段（Base64编码）或直接音频流
      let data = '';
      
      res.on('data', chunk => { data += chunk; });
      
      res.on('end', () => {
        try {
          const json = JSON.parse(data);
          
          if (json.base_resp && json.base_resp.status_code !== 0) {
            reject(new Error(`API错误: ${json.base_resp.status_code} - ${json.base_resp.status_msg}`));
            return;
          }
          
          // T2A v2 返回格式：{ audio_file: "base64编码的mp3" }
          if (json.audio_file) {
            const audioBuffer = Buffer.from(json.audio_file, 'base64');
            resolve(audioBuffer);
          } else if (json.data && json.data.audio_file) {
            const audioBuffer = Buffer.from(json.data.audio_file, 'base64');
            resolve(audioBuffer);
          } else {
            reject(new Error('API返回格式异常，未找到audio_file字段'));
          }
        } catch (e) {
          // 某些情况下可能直接返回二进制音频
          if (data.length > 1000 && !data.startsWith('{')) {
            resolve(Buffer.from(data, 'binary'));
          } else {
            reject(new Error(`解析响应失败: ${e.message}, 原始响应: ${data.substring(0, 200)}`));
          }
        }
      });
    });

    req.on('error', (e) => {
      reject(new Error(`网络请求失败: ${e.message}`));
    });

    req.write(body);
    req.end();
  });
}

// ==================== 主流程 ====================

async function main() {
  console.log('');
  console.log('========================================');
  console.log('   答辩语音自动生成器 - Minimax TTS');
  console.log('========================================');
  console.log('');

  // 检查配置
  if (!API_KEY) {
    console.error('❌ 错误：请先设置 MINIMAX_API_KEY 环境变量');
    console.error('');
    console.error('方法1（临时生效）：');
    console.error('  $env:MINIMAX_API_KEY="your_key"; $env:MINIMAX_APP_ID="your_app_id"');
    console.error('  node generate_voice.js');
    console.error('');
    console.error('方法2（永久生效，编辑当前用户的环境变量）：');
    console.error('  Windows: [系统属性] → [环境变量] → 新建用户变量');
    console.error('  macOS/Linux: 将下述行添加到 ~/.bashrc 或 ~/.zshrc:');
    console.error('  export MINIMAX_API_KEY="your_key"');
    console.error('  export MINIMAX_APP_ID="your_app_id"');
    console.error('');
    exit(1);
  }

  log(`API Key: ${API_KEY.substring(0, 8)}... (已设置)`);
  log(`App ID: ${APP_ID}`);
  log(`输出目录: ${AUDIO_DIR}`);
  log('');
  console.log('----------------------------------------');
  console.log('');

  let allSuccess = true;

  for (const item of OUTPUT_FILES) {
    const textFilePath = path.join(AUDIO_DIR, item.textFile);
    const outFilePath = path.join(AUDIO_DIR, item.outFile);

    // 检查文本文件是否存在
    if (!fs.existsSync(textFilePath)) {
      logError(new Error(`文本文件不存在: ${item.textFile}，跳过`));
      continue;
    }

    process.stdout.write(`📝 读取文本: ${item.textFile} ... `);
    const text = readTextFile(textFilePath);
    const charCount = text.length;
    console.log(`(${charCount} 字)`);

    if (charCount < 10) {
      logError(new Error('文本内容过少，跳过'));
      continue;
    }

    process.stdout.write(`🎙️  生成语音: ${item.outFile} ... `);

    try {
      // 分段处理（如果文本过长，每2000字为一段）
      const MAX_CHARS = 2000;
      let audioBuffers = [];

      if (text.length <= MAX_CHARS) {
        const buffer = await callMinimaxTTS(text);
        audioBuffers.push(buffer);
      } else {
        // 按段落分割
        const paragraphs = text.split(/\n\n+/);
        let currentChunk = '';
        let partNum = 1;

        for (const para of paragraphs) {
          if ((currentChunk + para).length > MAX_CHARS && currentChunk.length > 0) {
            process.stdout.write(`\n   [Part ${partNum}] `);
            const buffer = await callMinimaxTTS(currentChunk.trim());
            audioBuffers.push(buffer);
            partNum++;
            currentChunk = '';
          }
          currentChunk += (currentChunk ? '\n\n' : '') + para;
        }
        if (currentChunk.trim().length > 0) {
          process.stdout.write(`\n   [Part ${partNum}] `);
          const buffer = await callMinimaxTTS(currentChunk.trim());
          audioBuffers.push(buffer);
        }
      }

      // 合并所有片段
      const finalBuffer = Buffer.concat(audioBuffers);

      // 保存文件
      fs.writeFileSync(outFilePath, finalBuffer);
      const sizeKB = Math.round(finalBuffer.length / 1024);
      console.log(` ${sizeKB} KB ✅`);
      logSuccess(item.outFile);

    } catch (err) {
      console.log('');
      logError(err);
      allSuccess = false;
    }

    // 每个文件间隔1秒，避免API限流
    if (OUTPUT_FILES.indexOf(item) < OUTPUT_FILES.length - 1) {
      await new Promise(r => setTimeout(r, 1000));
    }
  }

  console.log('');
  console.log('----------------------------------------');
  console.log('');

  if (allSuccess) {
    console.log('========================================');
    console.log('   ✅ 全部语音生成完毕！');
    console.log('========================================');
    console.log('');
    console.log('生成文件清单：');
    for (const item of OUTPUT_FILES) {
      const outFilePath = path.join(AUDIO_DIR, item.outFile);
      if (fs.existsSync(outFilePath)) {
        const stats = fs.statSync(outFilePath);
        console.log(`  ✅ ${item.outFile} (${Math.round(stats.size / 1024)} KB)`);
      }
    }
    console.log('');
    console.log('可直接导入PPT或播放器使用！');
    console.log('');
  } else {
    console.log('⚠️ 部分文件生成失败，请检查上方错误信息');
    exit(1);
  }
}

main().catch(err => {
  console.error('Fatal error:', err);
  exit(1);
});
