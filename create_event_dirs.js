const fs = require('fs');
const path = require('path');

const basePath = 'c:\\Users\\Youssef\\Documents\\Projects\\SmartStock\\docs';

const directories = [
  'events',
  'events/identity',
  'events/product',
  'events/inventory',
  'events/warehouse',
  'events/supplier',
  'events/customer',
  'events/purchase-order',
  'events/sales-order'
];

console.log('Creating directories...');
console.log('='.repeat(70));

directories.forEach(dir => {
  const fullPath = path.join(basePath, dir);
  try {
    fs.mkdirSync(fullPath, { recursive: true });
    console.log(`✓ Created: ${fullPath}`);
  } catch (error) {
    console.log(`✗ Failed: ${fullPath}`);
    console.log(`  Error: ${error.message}`);
  }
});

console.log('\n' + '='.repeat(70));
console.log('Verifying all directories exist...');
console.log('='.repeat(70) + '\n');

let allExist = true;
directories.forEach(dir => {
  const fullPath = path.join(basePath, dir);
  const exists = fs.existsSync(fullPath) && fs.statSync(fullPath).isDirectory();
  const status = exists ? '✓' : '✗';
  console.log(`${status} ${fullPath}`);
  if (!exists) allExist = false;
});

console.log('\n' + '='.repeat(70));
if (allExist) {
  console.log('SUCCESS: All directories created successfully!');
} else {
  console.log('ERROR: Some directories are missing!');
  process.exit(1);
}

// List the directory structure
console.log('\nDirectory structure:');
console.log('='.repeat(70));

function listDirs(dir, indent = '') {
  try {
    const files = fs.readdirSync(dir);
    files.sort().forEach(file => {
      const fullPath = path.join(dir, file);
      const stat = fs.statSync(fullPath);
      if (stat.isDirectory()) {
        console.log(indent + file + '/');
        listDirs(fullPath, indent + '  ');
      }
    });
  } catch (error) {
    console.log('Error reading directory:', error.message);
  }
}

listDirs(path.join(basePath, 'events'));
